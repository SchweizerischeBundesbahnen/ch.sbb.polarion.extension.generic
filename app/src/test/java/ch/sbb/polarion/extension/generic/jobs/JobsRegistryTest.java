package ch.sbb.polarion.extension.generic.jobs;

import ch.sbb.polarion.extension.generic.rest.model.jobs.JobStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobsRegistryTest {

    private final List<String> removedJobIds = new ArrayList<>();
    private final AtomicInteger cleanupTimeout = new AtomicInteger();
    private final JobsRegistry<String, String> registry = JobsRegistry.<String, String>builder("Test job")
            .onJobRemoved(removedJobIds::add)
            .onCleanup(cleanupTimeout::set)
            .build();

    @AfterEach
    void tearDown() {
        registry.shutdown();
    }

    @Test
    void shouldKeepSettings() {
        assertThat(registry.getJobName()).isEqualTo("Test job");
        assertThat(registry.getTimeoutPolicy()).isEqualTo(TimeoutPolicy.INTERRUPT);
    }

    @Test
    void shouldDropOnlyJobsFinishedLongerThanTimeout() {
        AsyncJob<String, String> finished = register("finished");
        finished.finish(JobOutcome.succeeded("result"));
        AsyncJob<String, String> running = register("running");

        registry.cleanupExpiredJobs(30);
        assertThat(registry.getJob("finished")).isNotNull();

        // a moment later, when the finished job is older than a timeout of 0
        registry.cleanupExpiredJobs(0, Instant.now().plusSeconds(1));

        assertThat(registry.getJob("finished")).isNull();
        assertThat(registry.getJob("running")).isSameAs(running);
        assertThat(removedJobIds).containsExactly("finished");
        assertThat(cleanupTimeout).hasValue(0);
    }

    /**
     * A removal hook which fails for one job neither keeps the other expired jobs nor stops the cleanup hook, which
     * is where data left behind by the failed hook is swept.
     */
    @Test
    void shouldContinueCleanupWhenRemovalHookFails() {
        List<String> removed = new ArrayList<>();
        AtomicInteger cleanupRuns = new AtomicInteger();
        JobsRegistry<String, String> failingHookRegistry = JobsRegistry.<String, String>builder("Failing hook")
                .onJobRemoved(jobId -> {
                    if (jobId.equals("first")) {
                        throw new IllegalStateException("cannot remove data of " + jobId);
                    }
                    removed.add(jobId);
                })
                .onCleanup(timeout -> cleanupRuns.incrementAndGet())
                .build();
        try {
            for (String jobId : List.of("first", "second")) {
                AsyncJob<String, String> job = new AsyncJob<>(jobId, "user", null);
                failingHookRegistry.submit(job, () -> { }, () -> { }, 60);
                job.finish(JobOutcome.succeeded("result"));
            }

            failingHookRegistry.cleanupExpiredJobs(0, Instant.now().plusSeconds(1));

            assertThat(failingHookRegistry.getJobs()).isEmpty();
            assertThat(removed).containsExactly("second");
            assertThat(cleanupRuns).hasValue(1);
        } finally {
            failingHookRegistry.shutdown();
        }
    }

    @Test
    void shouldSurviveFailingCleanupHook() {
        JobsRegistry<String, String> failingHookRegistry = JobsRegistry.<String, String>builder("Failing hook")
                .onCleanup(timeout -> {
                    throw new IllegalStateException("sweep failed");
                })
                .build();
        try {
            assertThatCode(() -> failingHookRegistry.cleanupExpiredJobs(0)).doesNotThrowAnyException();
        } finally {
            failingHookRegistry.shutdown();
        }
    }

    @Test
    void shouldCountExpiryFromFinish() {
        AsyncJob<String, String> job = new AsyncJob<>("id", "user", null);
        Instant now = Instant.now();

        assertThat(job.isExpired(0, now.plusSeconds(1))).isFalse();
        job.finish(JobOutcome.succeeded("result"));
        assertThat(job.isExpired(1, now.plusSeconds(30))).isFalse();
        assertThat(job.isExpired(1, now.plusSeconds(120))).isTrue();
    }

    @Test
    void shouldStartCleanerOnce() {
        assertThat(registry.isCleanerRunning()).isFalse();

        registry.startCleaner(30);
        registry.startCleaner(30);
        assertThat(registry.isCleanerRunning()).isTrue();

        registry.stopCleaner();
        assertThat(registry.isCleanerRunning()).isFalse();
        registry.stopCleaner();
    }

    @Test
    void shouldRefuseNonPositiveCleanerTimeout() {
        assertThatThrownBy(() -> registry.startCleaner(0)).isInstanceOf(IllegalArgumentException.class);
        assertThat(registry.isCleanerRunning()).isFalse();
    }

    @Test
    void shouldCancelJobsOnClear() {
        AsyncJob<String, String> job = register("job");

        registry.clear();

        assertThat(job.toJobState().status()).isEqualTo(JobStatus.CANCELLED);
        assertThat(registry.getJobs()).isEmpty();
    }

    /**
     * A job asked for while the extension stops is refused, not registered, so no shutdown leaves it behind.
     */
    @Test
    void shouldRefuseJobAfterShutdown() {
        registry.shutdown();
        AsyncJob<String, String> job = new AsyncJob<>("late", "user", null);

        assertThatThrownBy(() -> registry.submit(job, () -> { }, () -> { }, 60))
                .isInstanceOf(RejectedExecutionException.class)
                .hasMessageContaining("the extension is stopping");
        assertThat(registry.getJob("late")).isNull();
    }

    @Test
    void shouldNotStartCleanerAfterShutdown() {
        registry.shutdown();

        registry.startCleaner(30);

        assertThat(registry.isCleanerRunning()).isFalse();
    }

    @Test
    void shouldStopCleanerOnShutdown() {
        registry.startCleaner(30);

        registry.shutdown();

        assertThat(registry.isCleanerRunning()).isFalse();
    }

    /**
     * A bounded registry runs and queues as many jobs as it is told to, and refuses the next one without keeping it.
     */
    @Test
    void shouldRefuseJobBeyondBounds() {
        JobsRegistry<String, String> bounded = JobsRegistry.<String, String>builder("Bounded").maxConcurrentJobs(1, 1).build();
        CountDownLatch release = new CountDownLatch(1);
        Runnable blocking = () -> {
            try {
                release.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        try {
            bounded.submit(new AsyncJob<>("running", "user", null), blocking, () -> { }, 60);
            bounded.submit(new AsyncJob<>("queued", "user", null), blocking, () -> { }, 60);
            AsyncJob<String, String> refused = new AsyncJob<>("refused", "user", null);

            assertThatThrownBy(() -> bounded.submit(refused, blocking, () -> { }, 60))
                    .isInstanceOf(RejectedExecutionException.class);
            assertThat(bounded.getJob("refused")).isNull();
            assertThat(bounded.getJobs()).hasSize(2);
        } finally {
            release.countDown();
            bounded.shutdown();
        }
    }

    @Test
    void shouldRefuseJobAsSoonAsThreadsAreBusyWithoutQueue() {
        JobsRegistry<String, String> bounded = JobsRegistry.<String, String>builder("Bounded").maxConcurrentJobs(1, 0).build();
        CountDownLatch release = new CountDownLatch(1);
        try {
            bounded.submit(new AsyncJob<>("running", "user", null), () -> {
                try {
                    release.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, () -> { }, 60);
            AsyncJob<String, String> refused = new AsyncJob<>("refused", "user", null);

            assertThatThrownBy(() -> bounded.submit(refused, () -> { }, () -> { }, 60))
                    .isInstanceOf(RejectedExecutionException.class);
        } finally {
            release.countDown();
            bounded.shutdown();
        }
    }

    @Test
    void shouldRefuseInvalidBounds() {
        JobsRegistry.Builder<String, String> builder = JobsRegistry.builder("Bounded");

        assertThatThrownBy(() -> builder.maxConcurrentJobs(0, 5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.maxConcurrentJobs(1, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    private AsyncJob<String, String> register(String jobId) {
        AsyncJob<String, String> job = new AsyncJob<>(jobId, "user", null);
        registry.submit(job, () -> { }, () -> { }, 60);
        return job;
    }
}
