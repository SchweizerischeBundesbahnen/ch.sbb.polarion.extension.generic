package ch.sbb.polarion.extension.generic.jobs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
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
    void shouldDropOnlyJobsFinishedLongerThanTimeout() throws Exception {
        AsyncJob<String, String> finished = register("finished");
        finished.getFuture().complete("result");
        AsyncJob<String, String> running = register("running");

        registry.cleanupExpiredJobs(30);
        assertThat(registry.getJob("finished")).isNotNull();

        Thread.sleep(5);
        registry.cleanupExpiredJobs(0);

        assertThat(registry.getJob("finished")).isNull();
        assertThat(registry.getJob("running")).isSameAs(running);
        assertThat(removedJobIds).containsExactly("finished");
        assertThat(cleanupTimeout).hasValue(0);
    }

    @Test
    void shouldCountExpiryFromFinish() {
        AsyncJob<String, String> job = new AsyncJob<>("id", "user", null);
        Instant now = Instant.now();

        assertThat(job.isExpired(0, now.plusSeconds(1))).isFalse();
        job.getFuture().complete("result");
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
        CompletableFuture<String> future = register("job").getFuture();

        registry.clear();

        assertThat(future.isCancelled()).isTrue();
        assertThat(registry.getJobs()).isEmpty();
    }

    @Test
    void shouldStopCleanerOnShutdown() {
        registry.startCleaner(30);

        registry.shutdown();

        assertThat(registry.isCleanerRunning()).isFalse();
    }

    private AsyncJob<String, String> register(String jobId) {
        AsyncJob<String, String> job = new AsyncJob<>(jobId, "user", null);
        registry.register(job, () -> { });
        return job;
    }
}
