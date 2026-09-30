package ch.sbb.polarion.extension.generic.jobs;

import ch.sbb.polarion.extension.generic.rest.filter.LogoutFilter;
import ch.sbb.polarion.extension.generic.rest.model.jobs.JobStatus;
import ch.sbb.polarion.extension.generic.util.RequestContextUtil;
import com.polarion.platform.security.ISecurityService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.security.auth.Subject;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncJobsServiceTest {

    private static final String TEST_USER = "testUser";
    private static final String PAYLOAD = "payload";

    @Mock
    private ISecurityService securityService;

    @Mock
    private Subject subject;

    @Mock
    private ServletRequestAttributes requestAttributes;

    private JobsRegistry<String, String> interruptRegistry;
    private JobsRegistry<String, String> cooperativeRegistry;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        interruptRegistry = JobsRegistry.<String, String>builder("Test")
                .timeoutPolicy(TimeoutPolicy.INTERRUPT)
                .timeoutUnit(TimeUnit.MILLISECONDS)
                .build();
        cooperativeRegistry = JobsRegistry.<String, String>builder("Test")
                .timeoutPolicy(TimeoutPolicy.COOPERATIVE)
                .timeoutUnit(TimeUnit.MILLISECONDS)
                .build();
        RequestContextHolder.setRequestAttributes(requestAttributes);
        lenient().when(securityService.getCurrentUser()).thenReturn(TEST_USER);
        lenient().when(securityService.getCurrentSubject()).thenReturn(subject);
        lenient().when(securityService.doAsUser(any(Subject.class), any(PrivilegedAction.class)))
                .thenAnswer(invocation -> ((PrivilegedAction<?>) invocation.getArgument(1)).run());
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        interruptRegistry.shutdown();
        cooperativeRegistry.shutdown();
    }

    @Test
    void shouldRunJobAsUserAndReturnResult() throws Exception {
        asyncRequest();
        AsyncJobsService<String, String> service = service(interruptRegistry);

        String jobId = service.startJob(PAYLOAD, 60, control -> "result of " + control.jobId());

        awaitDone(interruptRegistry, jobId);
        JobState jobState = service.getJobState(jobId);
        assertThat(jobState.status()).isEqualTo(JobStatus.SUCCESSFULLY_FINISHED);
        assertThat(jobState.errorMessage()).isNull();
        assertThat(service.getJobResult(jobId)).contains("result of " + jobId);
        assertThat(service.getJobPayload(jobId)).isEqualTo(PAYLOAD);
        assertThat(service.getAllJobsStates()).containsOnlyKeys(jobId);
        verify(securityService).doAsUser(eq(subject), any(PrivilegedAction.class));
        verify(securityService, timeout(1000)).logout(subject);
    }

    @Test
    void shouldHideJobFromOtherUsers() throws Exception {
        AsyncJobsService<String, String> service = service(interruptRegistry);
        String jobId = service.startJob(PAYLOAD, 60, control -> "result");
        awaitDone(interruptRegistry, jobId);

        when(securityService.getCurrentUser()).thenReturn("other_" + TEST_USER);

        assertThatThrownBy(() -> service.getJobState(jobId)).isInstanceOf(NoSuchElementException.class).hasMessage("Test job is unknown: " + jobId);
        assertThatThrownBy(() -> service.getJobResult(jobId)).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> service.getJobPayload(jobId)).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> service.cancelJob(jobId)).isInstanceOf(NoSuchElementException.class);
        assertThat(service.getAllJobsStates()).isEmpty();
        assertThatThrownBy(() -> service.getJobState("unknown")).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void shouldHideJobWithoutUserFromEverybody() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(null);
        AsyncJobsService<String, String> service = service(interruptRegistry);
        String jobId = service.startJob(PAYLOAD, 60, control -> "result");
        awaitDone(interruptRegistry, jobId);

        assertThatThrownBy(() -> service.getJobState(jobId)).isInstanceOf(NoSuchElementException.class);
        assertThat(service.getAllJobsStates()).isEmpty();
    }

    @Test
    void shouldNotLogoutWhenSessionIsNotKeptForJob() throws Exception {
        when(requestAttributes.getAttribute(LogoutFilter.XSRF_SKIP_LOGOUT, RequestAttributes.SCOPE_REQUEST)).thenReturn(Boolean.TRUE);
        AsyncJobsService<String, String> service = service(interruptRegistry);

        String jobId = service.startJob(PAYLOAD, 60, control -> "result");

        awaitDone(interruptRegistry, jobId);
        verify(securityService, never()).logout(any());
    }

    @Test
    void shouldNotLogoutWithoutRequest() throws Exception {
        RequestContextHolder.resetRequestAttributes();
        AsyncJobsService<String, String> service = service(interruptRegistry);

        String jobId = service.startJob(PAYLOAD, 60, control -> "result");

        awaitDone(interruptRegistry, jobId);
        verify(securityService, never()).logout(any());
    }

    @Test
    void shouldRunJobDirectlyWithoutSubject() throws Exception {
        when(securityService.getCurrentSubject()).thenReturn(null);
        AsyncJobsService<String, String> service = service(interruptRegistry);

        String jobId = service.startJob(PAYLOAD, 60, control -> "result");

        awaitDone(interruptRegistry, jobId);
        assertThat(service.getJobResult(jobId)).contains("result");
        verify(securityService, never()).doAsUser(any(), any(PrivilegedAction.class));
    }

    @Test
    void shouldReportFailure() throws Exception {
        asyncRequest();
        AsyncJobsService<String, String> service = service(interruptRegistry);

        String jobId = service.startJob(PAYLOAD, 60, control -> {
            throw new IllegalStateException("test error");
        });

        awaitDone(interruptRegistry, jobId);
        JobState jobState = service.getJobState(jobId);
        assertThat(jobState.status()).isEqualTo(JobStatus.FAILED);
        assertThat(jobState.errorMessage()).isEqualTo("test error");
        assertThatThrownBy(() -> service.getJobResult(jobId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Test job was cancelled or failed: test error");
        verify(securityService, timeout(1000)).logout(subject);
    }

    @Test
    void shouldReportClassOfFailureWithoutMessage() throws Exception {
        AsyncJobsService<String, String> service = service(interruptRegistry);

        String jobId = service.startJob(PAYLOAD, 60, control -> {
            throw new ConcurrentModificationException();
        });

        awaitDone(interruptRegistry, jobId);
        assertThat(service.getJobState(jobId).errorMessage()).isEqualTo(ConcurrentModificationException.class.getName());
    }

    @Test
    void shouldReportErrors() throws Exception {
        AsyncJobsService<String, String> service = service(interruptRegistry);

        String jobId = service.startJob(PAYLOAD, 60, control -> {
            throw new AssertionError("broken");
        });

        awaitDone(interruptRegistry, jobId);
        assertThat(service.getJobState(jobId).status()).isEqualTo(JobStatus.FAILED);
        assertThat(service.getJobState(jobId).errorMessage()).isEqualTo("broken");
    }

    @Test
    void shouldReturnEmptyResultWhileRunning() {
        CountDownLatch release = new CountDownLatch(1);
        AsyncJobsService<String, String> service = service(interruptRegistry);

        String jobId = service.startJob(PAYLOAD, 60_000, control -> {
            awaitQuietly(release);
            return "result";
        });

        assertThat(service.getJobResult(jobId)).isEmpty();
        assertThat(service.getJobState(jobId).status()).isEqualTo(JobStatus.IN_PROGRESS);
        release.countDown();
    }

    @Test
    void shouldReportProgress() throws Exception {
        CountDownLatch reported = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AsyncJobsService<String, String> service = service(cooperativeRegistry);

        String jobId = service.startJob(PAYLOAD, 60_000, control -> {
            control.reportProgress("step 1");
            reported.countDown();
            awaitQuietly(release);
            return "result";
        });

        assertThat(reported.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(service.getJobState(jobId).progressMessage()).isEqualTo("step 1");
        release.countDown();
        awaitDone(cooperativeRegistry, jobId);
    }

    @Test
    void shouldInterruptJobOnTimeout() throws Exception {
        AtomicBoolean interrupted = new AtomicBoolean();
        CountDownLatch stopped = new CountDownLatch(1);
        AsyncJobsService<String, String> service = service(interruptRegistry);

        String jobId = service.startJob(PAYLOAD, 50, control -> {
            try {
                blockUntilInterrupted();
            } catch (InterruptedException e) {
                interrupted.set(true);
                Thread.currentThread().interrupt();
            } finally {
                stopped.countDown();
            }
            throw new IllegalStateException("interrupted");
        });

        awaitDone(interruptRegistry, jobId);
        JobState jobState = service.getJobState(jobId);
        assertThat(jobState.status()).isEqualTo(JobStatus.FAILED);
        assertThat(jobState.errorMessage()).isEqualTo("Timeout after 50 min");
        assertThat(stopped.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(interrupted).isTrue();
        // the failure the interrupt caused does not replace the timeout
        assertThat(service.getJobState(jobId).errorMessage()).isEqualTo("Timeout after 50 min");
    }

    @Test
    void shouldCancelAndInterruptJob() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch stopped = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();
        AsyncJobsService<String, String> service = service(interruptRegistry);

        String jobId = service.startJob(PAYLOAD, 60_000, control -> {
            started.countDown();
            try {
                blockUntilInterrupted();
            } catch (InterruptedException e) {
                interrupted.set(true);
                Thread.currentThread().interrupt();
            } finally {
                stopped.countDown();
            }
            throw new IllegalStateException("interrupted");
        });
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();

        service.cancelJob(jobId);

        JobState jobState = service.getJobState(jobId);
        assertThat(jobState.status()).isEqualTo(JobStatus.CANCELLED);
        assertThat(jobState.errorMessage()).isEqualTo(JobMessages.CANCELLED_BY_USER);
        assertThat(stopped.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(interrupted).isTrue();
        assertThat(service.getJobState(jobId).errorMessage()).isEqualTo(JobMessages.CANCELLED_BY_USER);
        assertThatThrownBy(() -> service.getJobResult(jobId)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldIgnoreCancelOfFinishedJob() throws Exception {
        AsyncJobsService<String, String> service = service(interruptRegistry);
        String jobId = service.startJob(PAYLOAD, 60, control -> "result");
        awaitDone(interruptRegistry, jobId);

        service.cancelJob(jobId);

        assertThat(service.getJobState(jobId).status()).isEqualTo(JobStatus.SUCCESSFULLY_FINISHED);
        assertThat(service.getJobState(jobId).errorMessage()).isNull();
    }

    @Test
    void shouldAskCooperativeJobToStopOnTimeout() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        AsyncJobsService<String, String> service = service(cooperativeRegistry);

        String jobId = service.startJob(PAYLOAD, 50, control -> {
            while (!control.isAbortRequested()) {
                Thread.onSpinWait();
            }
            // still running after the deadline, until it stops by itself
            awaitQuietly(release);
            // a task that stops on its own may say so with the exception made for it
            throw new CancellationException("aborted");
        });

        AsyncJob<String, String> job = cooperativeRegistry.getJob(jobId);
        assertThat(job).isNotNull();
        long deadline = System.currentTimeMillis() + 5000;
        while (!job.isAbortRequested() && System.currentTimeMillis() < deadline) {
            Thread.onSpinWait();
        }
        assertThat(service.getJobState(jobId).status()).isEqualTo(JobStatus.IN_PROGRESS);

        release.countDown();
        awaitDone(cooperativeRegistry, jobId);
        JobState jobState = service.getJobState(jobId);
        assertThat(jobState.status()).isEqualTo(JobStatus.FAILED);
        assertThat(jobState.errorMessage()).isEqualTo("Timeout after 50 min");
    }

    @Test
    void shouldCancelCooperativeJob() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        AsyncJobsService<String, String> service = service(cooperativeRegistry);

        String jobId = service.startJob(PAYLOAD, 60_000, control -> {
            started.countDown();
            while (!control.isAbortRequested()) {
                Thread.onSpinWait();
            }
            throw new IllegalStateException("aborted");
        });
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();

        service.cancelJob(jobId);

        awaitDone(cooperativeRegistry, jobId);
        JobState jobState = service.getJobState(jobId);
        assertThat(jobState.status()).isEqualTo(JobStatus.CANCELLED);
        assertThat(jobState.errorMessage()).isEqualTo(JobMessages.CANCELLED_BY_USER);
    }

    @Test
    void shouldKeepResultOfCooperativeJobFinishedAfterCancel() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        AsyncJobsService<String, String> service = service(cooperativeRegistry);

        String jobId = service.startJob(PAYLOAD, 60_000, control -> {
            started.countDown();
            while (!control.isAbortRequested()) {
                Thread.onSpinWait();
            }
            return "partial result";
        });
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();

        service.cancelJob(jobId);

        awaitDone(cooperativeRegistry, jobId);
        JobState jobState = service.getJobState(jobId);
        assertThat(jobState.status()).isEqualTo(JobStatus.SUCCESSFULLY_FINISHED);
        assertThat(jobState.errorMessage()).isNull();
        assertThat(service.getJobResult(jobId)).contains("partial result");
    }

    /**
     * A cancel that races a cooperative job finishing with its result loses: the result counts, and the job does not
     * carry the cancel as its error. The race cannot be forced, so it is run often enough to be hit; the test cannot
     * fail on correct code, only miss a regression on a given run.
     */
    @Test
    void shouldNotReportCancelOfCooperativeJobThatFinishedWithResult() throws Exception {
        AsyncJobsService<String, String> service = service(cooperativeRegistry);
        int successfulJobs = 0;
        for (int attempt = 0; attempt < 2000; attempt++) {
            String jobId = service.startJob(PAYLOAD, 60_000, control -> "result");
            service.cancelJob(jobId);
            awaitDone(cooperativeRegistry, jobId);

            JobState jobState = service.getJobState(jobId);
            if (jobState.status() == JobStatus.SUCCESSFULLY_FINISHED) {
                successfulJobs++;
                assertThat(jobState.errorMessage()).as("error of successful job %d", attempt).isNull();
            }
        }
        assertThat(successfulJobs).isPositive();
    }

    @Test
    void shouldRefuseJobBeyondExecutorBounds() {
        asyncRequest();
        CountDownLatch release = new CountDownLatch(1);
        JobsRegistry<String, String> boundedRegistry = JobsRegistry.<String, String>builder("Bounded").maxConcurrentJobs(1, 0).build();
        try {
            AsyncJobsService<String, String> service = service(boundedRegistry);
            String runningJobId = service.startJob(PAYLOAD, 60, control -> {
                awaitQuietly(release);
                return "result";
            });

            assertThatThrownBy(() -> service.startJob(PAYLOAD, 60, control -> "result"))
                    .isInstanceOf(RejectedExecutionException.class);
            assertThat(service.getAllJobsStates()).containsOnlyKeys(runningJobId);
            // the refused job ends no session: the caller gives it back
            verify(securityService, never()).logout(any());
        } finally {
            release.countDown();
            boundedRegistry.shutdown();
        }
    }

    /**
     * A job that timed out while it waited for a thread is over for its caller, so it never runs. The session kept
     * alive for it is still ended.
     */
    @Test
    void shouldNotRunJobThatTimedOutWhileQueued() {
        asyncRequest();
        runQueuedJobThatIsOverBeforeItGetsAThread((service, queuedJobId) -> {
            long deadline = System.currentTimeMillis() + 5000;
            while (!service.getJobState(queuedJobId).isDone() && System.currentTimeMillis() < deadline) {
                Thread.onSpinWait();
            }
            assertThat(service.getJobState(queuedJobId).errorMessage()).isEqualTo("Timeout after 50 min");
        }, 50);
    }

    /**
     * A job its caller cancelled while it waited for a thread never runs either.
     */
    @Test
    void shouldNotRunJobCancelledWhileQueued() {
        asyncRequest();
        runQueuedJobThatIsOverBeforeItGetsAThread((service, queuedJobId) -> {
            service.cancelJob(queuedJobId);
            assertThat(service.getJobState(queuedJobId).status()).isEqualTo(JobStatus.CANCELLED);
        }, 60_000);
    }

    /**
     * Occupies the only thread of an executor, queues a second job behind it with the given timeout, ends that job
     * with the given action while it still waits, then frees the thread and checks the queued job did not run.
     */
    private void runQueuedJobThatIsOverBeforeItGetsAThread(BiConsumer<AsyncJobsService<String, String>, String> endQueuedJob, int queuedJobTimeout) {
        JobsRegistry<String, String> queueingRegistry = JobsRegistry.<String, String>builder("Queueing")
                .timeoutPolicy(TimeoutPolicy.INTERRUPT)
                .timeoutUnit(TimeUnit.MILLISECONDS)
                .maxConcurrentJobs(1, 10)
                .build();
        CountDownLatch release = new CountDownLatch(1);
        AtomicBoolean queuedJobRan = new AtomicBoolean();
        Subject[] subjects = separateRequests(2);
        try {
            AsyncJobsService<String, String> service = service(queueingRegistry);
            service.startJob(PAYLOAD, 60_000, control -> {
                awaitQuietly(release);
                return "result";
            });
            String queuedJobId = service.startJob(PAYLOAD, queuedJobTimeout, control -> {
                queuedJobRan.set(true);
                return "result";
            });

            endQueuedJob.accept(service, queuedJobId);
            release.countDown();

            // each session is ended: the one of the job which ran, and the one of the job which was over before it ran
            verifyEachLoggedOutOnce(subjects);
            assertThat(queuedJobRan).isFalse();
            assertThat(service.getJobState(queuedJobId).isDone()).isTrue();
        } finally {
            release.countDown();
            queueingRegistry.shutdown();
        }
    }

    /**
     * A job still waiting for a thread when the extension stops is dropped by the executor, so the shutdown cancels
     * it and ends the session kept alive for it. The job which ran ends its own session, once.
     */
    @Test
    void shouldCancelQueuedJobOnShutdown() {
        asyncRequest();
        JobsRegistry<String, String> queueingRegistry = JobsRegistry.<String, String>builder("Queueing").maxConcurrentJobs(1, 10).build();
        CountDownLatch started = new CountDownLatch(1);
        AtomicBoolean queuedJobRan = new AtomicBoolean();
        Subject[] subjects = separateRequests(2);
        AsyncJobsService<String, String> service = service(queueingRegistry);
        String runningJobId = service.startJob(PAYLOAD, 60, control -> {
            started.countDown();
            awaitQuietly(new CountDownLatch(1));
            return "result";
        });
        String queuedJobId = service.startJob(PAYLOAD, 60, control -> {
            queuedJobRan.set(true);
            return "result";
        });
        awaitQuietly(started);

        queueingRegistry.shutdown();

        JobState queuedJobState = service.getJobState(queuedJobId);
        assertThat(queuedJobState.status()).isEqualTo(JobStatus.CANCELLED);
        assertThat(queuedJobState.errorMessage()).isEqualTo(JobMessages.STOPPED);
        // the running job was interrupted and ended its own session; the shutdown ended the one of the queued job
        verifyEachLoggedOutOnce(subjects);
        assertThat(queuedJobRan).isFalse();
        assertThat(service.getJobState(runningJobId).isDone()).isTrue();
    }

    /**
     * A running cooperative job is asked to stop when the extension stops, and says that is why it stopped.
     */
    @Test
    void shouldAskRunningCooperativeJobToStopOnShutdown() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        AsyncJobsService<String, String> service = service(cooperativeRegistry);
        String jobId = service.startJob(PAYLOAD, 60_000, control -> {
            started.countDown();
            while (!control.isAbortRequested()) {
                Thread.onSpinWait();
            }
            throw new CancellationException("aborted");
        });
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();

        cooperativeRegistry.shutdown();

        awaitDone(cooperativeRegistry, jobId);
        JobState jobState = service.getJobState(jobId);
        assertThat(jobState.status()).isEqualTo(JobStatus.CANCELLED);
        assertThat(jobState.errorMessage()).isEqualTo(JobMessages.STOPPED);
    }

    /**
     * A job asked for while the extension stops is refused before it is registered or run.
     */
    @Test
    void shouldRefuseJobAfterShutdown() {
        AsyncJobsService<String, String> service = service(interruptRegistry);
        AtomicBoolean ran = new AtomicBoolean();
        interruptRegistry.shutdown();

        assertThatThrownBy(() -> service.startJob(PAYLOAD, 60, control -> {
            ran.set(true);
            return "result";
        })).isInstanceOf(RejectedExecutionException.class);
        assertThat(service.getAllJobsStates()).isEmpty();
        assertThat(ran).isFalse();
    }

    /**
     * The session is ended after the job is over, so a slow logout does not keep finished work "in progress".
     */
    @Test
    void shouldFinishJobBeforeSlowLogoutReturns() throws Exception {
        asyncRequest();
        CountDownLatch logoutStarted = new CountDownLatch(1);
        CountDownLatch releaseLogout = new CountDownLatch(1);
        doAnswer(invocation -> {
            logoutStarted.countDown();
            awaitQuietly(releaseLogout);
            return null;
        }).when(securityService).logout(subject);
        AsyncJobsService<String, String> service = service(interruptRegistry);

        String jobId = service.startJob(PAYLOAD, 60, control -> "result");

        try {
            assertThat(logoutStarted.await(5, TimeUnit.SECONDS)).isTrue();
            // the logout still runs, and the job is already over
            assertThat(service.getJobState(jobId).status()).isEqualTo(JobStatus.SUCCESSFULLY_FINISHED);
            assertThat(service.getJobResult(jobId)).contains("result");
        } finally {
            releaseLogout.countDown();
        }
    }

    /**
     * An error escaping the logout does not keep the job from ending: it is over before the logout runs.
     */
    @Test
    void shouldFinishJobWhenLogoutThrowsError() throws Exception {
        asyncRequest();
        doThrow(new LinkageError("logout broken")).when(securityService).logout(subject);
        AsyncJobsService<String, String> service = service(interruptRegistry);

        String jobId = service.startJob(PAYLOAD, 60, control -> "result");

        awaitDone(interruptRegistry, jobId);
        assertThat(service.getJobState(jobId).status()).isEqualTo(JobStatus.SUCCESSFULLY_FINISHED);
        verify(securityService, timeout(5000)).logout(subject);
    }

    /**
     * A task which restores the interrupt a stop sent must not leave it on the thread which then logs out.
     */
    @Test
    void shouldClearInterruptBeforeLogout() throws Exception {
        asyncRequest();
        AtomicBoolean interruptedAtLogout = new AtomicBoolean(true);
        CountDownLatch loggedOut = new CountDownLatch(1);
        doAnswer(invocation -> {
            interruptedAtLogout.set(Thread.currentThread().isInterrupted());
            loggedOut.countDown();
            return null;
        }).when(securityService).logout(subject);
        CountDownLatch started = new CountDownLatch(1);
        AsyncJobsService<String, String> service = service(interruptRegistry);
        String jobId = service.startJob(PAYLOAD, 60_000, control -> {
            started.countDown();
            try {
                blockUntilInterrupted();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("interrupted");
        });
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();

        service.cancelJob(jobId);

        assertThat(loggedOut.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(interruptedAtLogout).isFalse();
    }

    /**
     * With {@link TimeoutPolicy#INTERRUPT} a shutdown declares a running job over at once, as a timeout would, even if
     * the job does not react to its interrupt.
     */
    @Test
    void shouldCancelRunningInterruptJobAtOnceOnShutdown() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AsyncJobsService<String, String> service = service(interruptRegistry);
        String jobId = service.startJob(PAYLOAD, 60_000, control -> {
            started.countDown();
            // ignores interrupts, like a call stuck in I/O
            while (release.getCount() > 0) {
                Thread.onSpinWait();
            }
            return "result";
        });
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
        try {
            interruptRegistry.shutdown();

            JobState jobState = service.getJobState(jobId);
            assertThat(jobState.status()).isEqualTo(JobStatus.CANCELLED);
            assertThat(jobState.errorMessage()).isEqualTo(JobMessages.STOPPED);
        } finally {
            release.countDown();
        }
    }

    /**
     * With {@link TimeoutPolicy#COOPERATIVE} a shutdown only asks a running job to stop: its thread may be writing, so
     * it is not interrupted.
     */
    @Test
    void shouldNotInterruptRunningCooperativeJobOnShutdown() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();
        AsyncJobsService<String, String> service = service(cooperativeRegistry);
        String jobId = service.startJob(PAYLOAD, 60_000, control -> {
            started.countDown();
            while (!control.isAbortRequested()) {
                Thread.onSpinWait();
            }
            // give an interrupt which a shutdown would send the time to arrive
            long until = System.currentTimeMillis() + 200;
            while (System.currentTimeMillis() < until) {
                interrupted.compareAndSet(false, Thread.currentThread().isInterrupted());
                Thread.onSpinWait();
            }
            throw new CancellationException("stopped at a safe point");
        });
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();

        cooperativeRegistry.shutdown();

        awaitDone(cooperativeRegistry, jobId);
        assertThat(interrupted).isFalse();
        assertThat(service.getJobState(jobId).errorMessage()).isEqualTo(JobMessages.STOPPED);
    }

    /**
     * With {@link TimeoutPolicy#COOPERATIVE} a job which times out while it waits for a thread has written nothing,
     * so it ends at once and never runs.
     */
    @Test
    void shouldEndQueuedCooperativeJobAtItsDeadline() throws Exception {
        JobsRegistry<String, String> queueingRegistry = JobsRegistry.<String, String>builder("Queueing")
                .timeoutPolicy(TimeoutPolicy.COOPERATIVE)
                .timeoutUnit(TimeUnit.MILLISECONDS)
                .maxConcurrentJobs(1, 1)
                .build();
        CountDownLatch release = new CountDownLatch(1);
        AtomicBoolean queuedJobRan = new AtomicBoolean();
        try {
            AsyncJobsService<String, String> service = service(queueingRegistry);
            service.startJob(PAYLOAD, 60_000, control -> {
                awaitQuietly(release);
                return "result";
            });
            String queuedJobId = service.startJob(PAYLOAD, 50, control -> {
                queuedJobRan.set(true);
                return "result";
            });

            awaitDone(queueingRegistry, queuedJobId);
            JobState jobState = service.getJobState(queuedJobId);
            assertThat(jobState.status()).isEqualTo(JobStatus.FAILED);
            assertThat(jobState.errorMessage()).isEqualTo("Timeout after 50 min");
            release.countDown();
            assertThat(queuedJobRan).isFalse();
        } finally {
            release.countDown();
            queueingRegistry.shutdown();
        }
    }

    /**
     * With {@link TimeoutPolicy#COOPERATIVE} a job stopped when it could not be taken back from the queue any more -
     * the executor had already handed it to a thread which has not reached the job yet - ends when that thread
     * reaches it, without running: it has done nothing yet.
     * <p>
     * The moment between the hand-over and the thread reaching the job cannot be timed from a test, so the stop is
     * recorded on the queued job directly, the way such a stop leaves it.
     */
    @Test
    void shouldNotRunCooperativeJobStoppedAfterItWasHandedToThread() throws Exception {
        asyncRequest();
        JobsRegistry<String, String> wrappedRegistry = JobsRegistry.<String, String>builder("Queueing")
                .timeoutPolicy(TimeoutPolicy.COOPERATIVE)
                .maxConcurrentJobs(1, 10)
                .build();
        CountDownLatch release = new CountDownLatch(1);
        AtomicBoolean queuedJobRan = new AtomicBoolean();
        Subject[] subjects = separateRequests(2);
        try {
            AsyncJobsService<String, String> service = service(wrappedRegistry);
            service.startJob(PAYLOAD, 60, control -> {
                awaitQuietly(release);
                return "result";
            });
            String queuedJobId = service.startJob(PAYLOAD, 60, control -> {
                queuedJobRan.set(true);
                return "result";
            });

            AsyncJob<String, String> queuedJob = wrappedRegistry.getJob(queuedJobId);
            assertThat(queuedJob).isNotNull();
            queuedJob.requestStop(StopRequest.cancel());
            assertThat(service.getJobState(queuedJobId).status()).isEqualTo(JobStatus.IN_PROGRESS);
            release.countDown();

            awaitDone(wrappedRegistry, queuedJobId);
            JobState jobState = service.getJobState(queuedJobId);
            assertThat(jobState.status()).isEqualTo(JobStatus.CANCELLED);
            assertThat(jobState.errorMessage()).isEqualTo(JobMessages.CANCELLED_BY_USER);
            assertThat(queuedJobRan).isFalse();
            // both jobs ended their sessions: the one which ran, and the one which found itself stopped
            verifyEachLoggedOutOnce(subjects);
        } finally {
            release.countDown();
            wrappedRegistry.shutdown();
        }
    }

    /**
     * A job cancelled while it waits is taken out of the queue, so its place is free for the next job at once.
     */
    @Test
    void shouldFreeQueuePlaceOfCancelledJob() {
        JobsRegistry<String, String> boundedRegistry = JobsRegistry.<String, String>builder("Bounded").maxConcurrentJobs(1, 1).build();
        CountDownLatch release = new CountDownLatch(1);
        try {
            AsyncJobsService<String, String> service = service(boundedRegistry);
            service.startJob(PAYLOAD, 60, control -> {
                awaitQuietly(release);
                return "result";
            });
            String queuedJobId = service.startJob(PAYLOAD, 60, control -> "result");

            service.cancelJob(queuedJobId);

            assertThat(service.getJobState(queuedJobId).status()).isEqualTo(JobStatus.CANCELLED);
            assertThatCode(() -> service.startJob(PAYLOAD, 60, control -> "result")).doesNotThrowAnyException();
        } finally {
            release.countDown();
            boundedRegistry.shutdown();
        }
    }

    @Test
    void shouldFailJobWithoutResult() throws Exception {
        AsyncJobsService<String, String> service = service(interruptRegistry);

        String jobId = service.startJob(PAYLOAD, 60, control -> null);

        awaitDone(interruptRegistry, jobId);
        JobState jobState = service.getJobState(jobId);
        assertThat(jobState.status()).isEqualTo(JobStatus.FAILED);
        assertThat(jobState.errorMessage()).isEqualTo(JobMessages.NO_RESULT);
    }

    /**
     * An overridden description which fails itself does not keep the failed job from ending.
     */
    @Test
    void shouldEndJobWhoseFailureCannotBeDescribed() throws Exception {
        AsyncJobsService<String, String> service = new AsyncJobsService<>(interruptRegistry, securityService) {
            @Override
            protected @NotNull String describeFailure(@NotNull Throwable thrown) {
                throw new IllegalArgumentException("cannot describe");
            }
        };

        String jobId = service.startJob(PAYLOAD, 60, control -> {
            throw new IllegalStateException("broken");
        });

        awaitDone(interruptRegistry, jobId);
        JobState jobState = service.getJobState(jobId);
        assertThat(jobState.status()).isEqualTo(JobStatus.FAILED);
        assertThat(jobState.errorMessage()).isEqualTo(IllegalStateException.class.getName());
    }

    /**
     * Not even an {@link Error} from an overridden description keeps the failed job from ending.
     */
    @Test
    void shouldEndJobWhoseFailureDescriptionThrowsError() throws Exception {
        AsyncJobsService<String, String> service = new AsyncJobsService<>(interruptRegistry, securityService) {
            @Override
            protected @NotNull String describeFailure(@NotNull Throwable thrown) {
                throw new StackOverflowError("cannot describe");
            }
        };

        String jobId = service.startJob(PAYLOAD, 60, control -> {
            throw new IllegalStateException("broken");
        });

        awaitDone(interruptRegistry, jobId);
        JobState jobState = service.getJobState(jobId);
        assertThat(jobState.status()).isEqualTo(JobStatus.FAILED);
        assertThat(jobState.errorMessage()).isEqualTo(IllegalStateException.class.getName());
    }

    /**
     * A session which cannot be ended when the extension stops does not keep the other waiting jobs from being ended
     * and logged out. The error is not lost: it comes out of the shutdown, after the last job.
     */
    @Test
    void shouldEndAllQueuedJobsOnShutdownWhenOneLogoutFails() {
        asyncRequest();
        // the first logout of all fails, whichever queued job the shutdown ends first
        doThrow(new LinkageError("logout broken")).doNothing().when(securityService).logout(any());
        JobsRegistry<String, String> queueingRegistry = JobsRegistry.<String, String>builder("Queueing").maxConcurrentJobs(1, 10).build();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Subject[] subjects = separateRequests(3);
        try {
            AsyncJobsService<String, String> service = service(queueingRegistry);
            service.startJob(PAYLOAD, 60, control -> {
                started.countDown();
                // ignores the interrupt of the shutdown, so that the queued jobs are logged out first
                while (release.getCount() > 0) {
                    Thread.onSpinWait();
                }
                return "result";
            });
            String firstQueuedJobId = service.startJob(PAYLOAD, 60, control -> "result");
            String secondQueuedJobId = service.startJob(PAYLOAD, 60, control -> "result");
            awaitQuietly(started);

            assertThatThrownBy(queueingRegistry::shutdown).isInstanceOf(LinkageError.class);

            assertThat(service.getJobState(firstQueuedJobId).status()).isEqualTo(JobStatus.CANCELLED);
            assertThat(service.getJobState(secondQueuedJobId).status()).isEqualTo(JobStatus.CANCELLED);
            // both queued jobs were logged out, the first of them with the error
            verify(securityService).logout(same(subjects[1]));
            verify(securityService).logout(same(subjects[2]));
        } finally {
            release.countDown();
            queueingRegistry.shutdown();
        }
    }

    /**
     * A started job owns the session kept for it: the request records it, so that a later failure of the request does
     * not give the session back under the running job.
     */
    @Test
    void shouldMarkRequestOnlyWhenJobStarted() {
        AsyncJobsService<String, String> service = service(interruptRegistry);

        service.startJob(PAYLOAD, 60, control -> "result");
        verify(requestAttributes).setAttribute(RequestContextUtil.ASYNC_JOB_STARTED, Boolean.TRUE, RequestAttributes.SCOPE_REQUEST);

        interruptRegistry.shutdown();
        clearInvocations(requestAttributes);
        assertThatThrownBy(() -> service.startJob(PAYLOAD, 60, control -> "result")).isInstanceOf(JobsRegistryShutDownException.class);
        verify(requestAttributes, never()).setAttribute(eq(RequestContextUtil.ASYNC_JOB_STARTED), any(), anyInt());
    }

    /**
     * A deadline and a cancel which reach a job together: the one which came first decides how the job ends, whoever
     * of them then ends it. The deadline's request is recorded directly, the way a deadline which got there first but
     * has not ended the job yet leaves it.
     */
    @Test
    void shouldEndWithFirstStopRequestWhenCancelComesSecond() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        AsyncJobsService<String, String> service = service(interruptRegistry);
        String jobId = service.startJob(PAYLOAD, 60_000, control -> {
            started.countDown();
            try {
                blockUntilInterrupted();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("interrupted");
        });
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
        AsyncJob<String, String> job = interruptRegistry.getJob(jobId);
        assertThat(job).isNotNull();
        job.requestStop(StopRequest.timeout(5));

        service.cancelJob(jobId);

        JobState jobState = service.getJobState(jobId);
        assertThat(jobState.status()).isEqualTo(JobStatus.FAILED);
        assertThat(jobState.errorMessage()).isEqualTo("Timeout after 5 min");
    }

    /**
     * A shutdown after a cancel reports the cancel: the first request decides how a job ends.
     */
    @Test
    void shouldReportEarlierCancelOnShutdown() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AsyncJobsService<String, String> service = service(interruptRegistry);
        String jobId = service.startJob(PAYLOAD, 60_000, control -> {
            started.countDown();
            while (release.getCount() > 0) {
                Thread.onSpinWait();
            }
            return "result";
        });
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
        try {
            AsyncJob<String, String> job = interruptRegistry.getJob(jobId);
            assertThat(job).isNotNull();
            job.requestStop(StopRequest.cancel());

            interruptRegistry.shutdown();

            JobState jobState = service.getJobState(jobId);
            assertThat(jobState.status()).isEqualTo(JobStatus.CANCELLED);
            assertThat(jobState.errorMessage()).isEqualTo(JobMessages.CANCELLED_BY_USER);
        } finally {
            release.countDown();
        }
    }

    /**
     * Two jobs of one request share its session: the first to finish does not end it under the other, and the last
     * ends it, once.
     */
    @Test
    void shouldEndSharedSessionWithLastJobOfRequest() throws Exception {
        asyncRequest();
        CountDownLatch release = new CountDownLatch(1);
        AsyncJobsService<String, String> service = service(interruptRegistry);
        // this registry counts timeouts in milliseconds: long enough for the long job not to be stopped
        String longJobId = service.startJob(PAYLOAD, 60_000, control -> {
            awaitQuietly(release);
            return "result";
        });
        String shortJobId = service.startJob(PAYLOAD, 60_000, control -> "result");

        awaitDone(interruptRegistry, shortJobId);
        verify(securityService, after(200).never()).logout(any());

        release.countDown();
        awaitDone(interruptRegistry, longJobId);
        verify(securityService, timeout(5000)).logout(subject);
        verify(securityService, after(200).times(1)).logout(subject);
    }

    /**
     * A second job of a request which is refused takes nothing from the session the first job still uses.
     */
    @Test
    void shouldKeepSharedSessionWhenSecondJobIsRefused() throws Exception {
        asyncRequest();
        JobsRegistry<String, String> boundedRegistry = JobsRegistry.<String, String>builder("Bounded").maxConcurrentJobs(1, 0).build();
        CountDownLatch release = new CountDownLatch(1);
        try {
            AsyncJobsService<String, String> service = service(boundedRegistry);
            String jobId = service.startJob(PAYLOAD, 60, control -> {
                awaitQuietly(release);
                return "result";
            });
            assertThatThrownBy(() -> service.startJob(PAYLOAD, 60, control -> "result")).isInstanceOf(RejectedExecutionException.class);
            verify(securityService, never()).logout(any());

            release.countDown();
            awaitDone(boundedRegistry, jobId);
            verify(securityService, timeout(5000)).logout(subject);
        } finally {
            release.countDown();
            boundedRegistry.shutdown();
        }
    }

    @Test
    void shouldRefuseJobAfterShutdownAsShutDown() {
        AsyncJobsService<String, String> service = service(interruptRegistry);
        interruptRegistry.shutdown();

        assertThatThrownBy(() -> service.startJob(PAYLOAD, 60, control -> "result"))
                .isInstanceOf(JobsRegistryShutDownException.class);
    }

    @Test
    void shouldRefuseNonPositiveTimeout() {
        AsyncJobsService<String, String> service = service(interruptRegistry);

        assertThatThrownBy(() -> service.startJob(PAYLOAD, 0, control -> "result")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldUnwrapFutureWrappers() {
        IllegalStateException cause = new IllegalStateException("cause");

        assertThat(AsyncJobsService.rootReason(new CompletionException(new ExecutionException(cause)))).isSameAs(cause);
        assertThat(AsyncJobsService.rootReason(new CompletionException(null))).isInstanceOf(CompletionException.class);
        assertThat(service(interruptRegistry).describeFailure(new CompletionException(cause))).isEqualTo("cause");
    }

    private AsyncJobsService<String, String> service(JobsRegistry<String, String> registry) {
        return new AsyncJobsService<>(registry, securityService);
    }

    /**
     * Jobs started by separate requests: each has a subject, and so a session, of its own - unlike jobs started by one
     * request, which share its session.
     *
     * @return the subjects, in the order the jobs are started
     */
    private Subject[] separateRequests(int count) {
        Subject[] subjects = new Subject[count];
        for (int index = 0; index < count; index++) {
            subjects[index] = new Subject();
        }
        when(securityService.getCurrentSubject()).thenReturn(subjects[0], Arrays.copyOfRange(subjects, 1, count));
        return subjects;
    }

    private void verifyEachLoggedOutOnce(Subject... subjects) {
        for (Subject each : subjects) {
            verify(securityService, timeout(5000)).logout(same(each));
        }
    }

    private void asyncRequest() {
        when(requestAttributes.getAttribute(LogoutFilter.XSRF_SKIP_LOGOUT, RequestAttributes.SCOPE_REQUEST)).thenReturn(Boolean.FALSE);
        when(requestAttributes.getAttribute(LogoutFilter.ASYNC_SKIP_LOGOUT, RequestAttributes.SCOPE_REQUEST)).thenReturn(Boolean.TRUE);
    }

    static void awaitDone(JobsRegistry<?, ?> registry, String jobId) throws Exception {
        AsyncJob<?, ?> job = registry.getJob(jobId);
        assertThat(job).isNotNull();
        job.completion().get(5, TimeUnit.SECONDS);
    }

    /**
     * Blocks the way a long conversion does, until its thread is interrupted. The latch is never released.
     */
    private static void blockUntilInterrupted() throws InterruptedException {
        assertThat(new CountDownLatch(1).await(10, TimeUnit.SECONDS)).as("interrupted before this").isFalse();
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
