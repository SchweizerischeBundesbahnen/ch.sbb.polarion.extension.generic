package ch.sbb.polarion.extension.generic.jobs;

import ch.sbb.polarion.extension.generic.rest.filter.LogoutFilter;
import ch.sbb.polarion.extension.generic.rest.model.jobs.JobStatus;
import com.polarion.platform.security.ISecurityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.security.auth.Subject;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Checks the invariants of the jobs framework on many jobs at once, with the parties which can end a job - the worker,
 * a deadline, a cancel, a shutdown - acting at random moments, for each policy and each kind of executor.
 * <p>
 * The invariants:
 * <ol>
 *     <li>once nothing runs any more, every job is over - none is left in progress;</li>
 *     <li>a job's status and error message belong together: a successful job has a result and no error, any other
 *     has an error message;</li>
 *     <li>the session kept for a job is ended exactly once, and none for a job which was refused;</li>
 *     <li>a task runs at most once;</li>
 *     <li>with {@link TimeoutPolicy#COOPERATIVE}, no task is interrupted.</li>
 * </ol>
 * Each run is driven by a fixed seed, which every failure names, so that a failing run can be repeated.
 */
class AsyncJobsInvariantsStressTest {

    private static final int JOBS_PER_RUN = 1000;
    private static final long[] SEEDS = {1L, 42L, 2026L};

    private final AtomicReference<Subject> currentSubject = new AtomicReference<>();
    // by identity: Subject.equals holds for any two subjects with the same principals, and these have none
    private final Map<Subject, AtomicInteger> logouts = Collections.synchronizedMap(new IdentityHashMap<>());
    private final AtomicInteger unexpectedLogouts = new AtomicInteger();

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    static Stream<Arguments> runs() {
        List<Arguments> runs = new ArrayList<>();
        for (TimeoutPolicy policy : TimeoutPolicy.values()) {
            for (int[] bounds : new int[][]{null, {2, 5}, {1, 0}}) {
                for (long seed : SEEDS) {
                    runs.add(Arguments.of(policy, bounds == null ? "unbounded" : bounds[0] + " running, " + bounds[1] + " queued", bounds, seed));
                }
            }
        }
        return runs.stream();
    }

    @ParameterizedTest(name = "{0}, {1}, seed {3}")
    @MethodSource("runs")
    void invariantsHold(TimeoutPolicy policy, String executorName, int[] bounds, long seed) throws Exception {
        JobsRegistry.Builder<String, String> builder = JobsRegistry.<String, String>builder("Stress")
                .timeoutPolicy(policy)
                .timeoutUnit(TimeUnit.MILLISECONDS);
        if (bounds != null) {
            builder.maxConcurrentJobs(bounds[0], bounds[1]);
        }
        JobsRegistry<String, String> registry = builder.build();
        AsyncJobsService<String, String> service = new AsyncJobsService<>(registry, securityService());
        RequestContextHolder.setRequestAttributes(asyncRequest());

        Random random = new Random(seed);
        ExecutorService stoppers = Executors.newFixedThreadPool(6);
        List<StartedJob> started = new ArrayList<>();
        List<Subject> refused = new ArrayList<>();
        int shutdownAt = JOBS_PER_RUN / 2 + random.nextInt(JOBS_PER_RUN / 2);
        try {
            for (int index = 0; index < JOBS_PER_RUN; index++) {
                if (index == shutdownAt) {
                    // Concurrently with the starts which follow, and with a burst of cancels of the jobs started so
                    // far: a cancel and a shutdown which reach the same waiting job together is the narrowest race.
                    List<String> startedSoFar = started.stream().map(StartedJob::jobId).toList();
                    CountDownLatch go = new CountDownLatch(1);
                    for (int canceller = 0; canceller < 3; canceller++) {
                        int first = canceller;
                        stoppers.execute(() -> {
                            awaitQuietly(go);
                            for (int job = startedSoFar.size() - 1 - first; job >= 0; job -= 3) {
                                service.cancelJob(startedSoFar.get(job));
                            }
                        });
                    }
                    stoppers.execute(() -> {
                        awaitQuietly(go);
                        registry.shutdown();
                    });
                    go.countDown();
                }
                Subject subject = new Subject();
                currentSubject.set(subject);
                TaskProbe probe = new TaskProbe();
                JobTask<String> task = task(random.nextInt(5), probe);
                int timeout = 1 + random.nextInt(30);
                try {
                    String jobId = service.startJob(null, timeout, task);
                    started.add(new StartedJob(jobId, subject, probe));
                    if (random.nextInt(2) == 0) {
                        long cancelDelayNanos = TimeUnit.MICROSECONDS.toNanos(random.nextInt(3000));
                        stoppers.execute(() -> {
                            LockSupport.parkNanos(cancelDelayNanos);
                            service.cancelJob(jobId);
                        });
                    }
                } catch (RejectedExecutionException e) {
                    refused.add(subject);
                }
            }
        } finally {
            stoppers.shutdown();
            assertThat(stoppers.awaitTermination(30, TimeUnit.SECONDS)).as("stoppers done, seed %d", seed).isTrue();
            registry.shutdown();
        }
        assertThat(registry.awaitTermination(30, TimeUnit.SECONDS)).as("registry threads done, seed %d", seed).isTrue();

        String run = "%s, %s, seed %d".formatted(policy, executorName, seed);
        for (StartedJob job : started) {
            JobState jobState = service.getJobState(job.jobId());
            String described = "%s: job %s ended %s with '%s'".formatted(run, job.jobId(), jobState.status(), jobState.errorMessage());
            assertThat(jobState.isDone()).as("1. over: %s", described).isTrue();
            if (jobState.status() == JobStatus.SUCCESSFULLY_FINISHED) {
                assertThat(jobState.errorMessage()).as("2. no error with a result: %s", described).isNull();
                assertThat(service.getJobResult(job.jobId())).as("2. a result: %s", described).isPresent();
            } else {
                assertThat(jobState.errorMessage()).as("2. an error without a result: %s", described).isNotBlank();
            }
            assertThat(logouts.getOrDefault(job.subject(), new AtomicInteger()).get()).as("3. one logout: %s", described).isEqualTo(1);
            assertThat(job.probe().runs.get()).as("4. runs at most once: %s", described).isLessThanOrEqualTo(1);
            if (policy == TimeoutPolicy.COOPERATIVE) {
                assertThat(job.probe().interrupted).as("5. not interrupted: %s", described).isFalse();
            }
        }
        for (Subject subject : refused) {
            assertThat(logouts.get(subject)).as("3. no logout for a refused job: %s", run).isNull();
        }
        assertThat(unexpectedLogouts).as("3. no logout of a subject without a job: %s", run).hasValue(0);
        assertThat(started).as("some jobs started: %s", run).isNotEmpty();
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * The kinds of task: quick, failing, without a result, waiting until it is asked to stop, and one which does not
     * check for a stop at all for a moment.
     */
    private static JobTask<String> task(int kind, TaskProbe probe) {
        return control -> {
            probe.runs.incrementAndGet();
            probe.observeInterrupt();
            return switch (kind) {
                case 0 -> "result";
                case 1 -> throw new IllegalStateException("failed on its own");
                case 2 -> null;
                case 3 -> {
                    // stops when asked to; also when interrupted, as a blocking call of an exporter would
                    long until = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(50);
                    while (System.nanoTime() < until) {
                        probe.observeInterrupt();
                        if (control.isAbortRequested() || Thread.currentThread().isInterrupted()) {
                            throw new CancellationException("stopped as asked");
                        }
                        Thread.onSpinWait();
                    }
                    yield "result after waiting";
                }
                default -> {
                    long until = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(2);
                    while (System.nanoTime() < until) {
                        probe.observeInterrupt();
                        Thread.onSpinWait();
                    }
                    yield "result without checking";
                }
            };
        };
    }

    @SuppressWarnings("unchecked")
    private ISecurityService securityService() {
        ISecurityService securityService = mock(ISecurityService.class, withSettings().stubOnly());
        when(securityService.getCurrentUser()).thenReturn("user");
        when(securityService.getCurrentSubject()).thenAnswer(invocation -> currentSubject.get());
        when(securityService.doAsUser(any(Subject.class), any(PrivilegedAction.class)))
                .thenAnswer(invocation -> ((PrivilegedAction<?>) invocation.getArgument(1)).run());
        doAnswer(invocation -> {
            Subject subject = invocation.getArgument(0);
            if (subject == null) {
                unexpectedLogouts.incrementAndGet();
            } else {
                synchronized (logouts) {
                    logouts.computeIfAbsent(subject, key -> new AtomicInteger()).incrementAndGet();
                }
            }
            return null;
        }).when(securityService).logout(any());
        return securityService;
    }

    /**
     * A call which authenticated itself and asked to keep its session for the job, so every job ends its session.
     */
    private static RequestAttributes asyncRequest() {
        ServletRequestAttributes requestAttributes = mock(ServletRequestAttributes.class, withSettings().stubOnly());
        when(requestAttributes.getAttribute(LogoutFilter.ASYNC_SKIP_LOGOUT, RequestAttributes.SCOPE_REQUEST)).thenReturn(Boolean.TRUE);
        return requestAttributes;
    }

    private record StartedJob(String jobId, Subject subject, TaskProbe probe) {
    }

    private static final class TaskProbe {
        private final AtomicInteger runs = new AtomicInteger();
        private final AtomicBoolean interrupted = new AtomicBoolean();

        void observeInterrupt() {
            if (Thread.currentThread().isInterrupted()) {
                interrupted.set(true);
            }
        }
    }
}
