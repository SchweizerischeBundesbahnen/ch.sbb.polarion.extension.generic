package ch.sbb.polarion.extension.generic.jobs;

import ch.sbb.polarion.extension.generic.util.NamedDaemonThreadFactory;
import com.polarion.core.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Long-lived state of one kind of asynchronous job: the jobs, the threads that run them, their deadlines and the cleaner
 * that drops old results.
 * <p>
 * REST controllers are created per request, so the jobs must outlive the {@link AsyncJobsService} that started them.
 * Keep exactly one registry per job kind, in a {@code static final} field of the extension:
 * <pre>{@code
 * private static final JobsRegistry<ExportParams, byte[]> REGISTRY = JobsRegistry.<ExportParams, byte[]>builder("PDF conversion")
 *         .timeoutPolicy(TimeoutPolicy.INTERRUPT)
 *         .build();
 * }</pre>
 * Start the cleaner once, when the extension starts, and call {@link #shutdown()} when it stops.
 *
 * @param <P> type of the payload stored with each job, for example its parameters
 * @param <R> type of the job result
 */
public final class JobsRegistry<P, R> {

    private static final Logger logger = Logger.getLogger(JobsRegistry.class);

    private final Map<String, AsyncJob<P, R>> jobs = new ConcurrentHashMap<>();
    private final @NotNull String jobName;
    private final @NotNull String threadNamePrefix;
    private final @NotNull TimeoutPolicy timeoutPolicy;
    private final @NotNull ExecutorService executor;
    private final @NotNull ScheduledExecutorService deadlineScheduler;
    private final @NotNull Consumer<String> onJobRemoved;
    private final @NotNull IntConsumer onCleanup;
    private final @NotNull TimeUnit timeoutUnit;
    private @Nullable ScheduledExecutorService cleanerService;

    private JobsRegistry(@NotNull Builder<P, R> builder) {
        this.jobName = builder.jobName;
        this.timeoutPolicy = builder.timeoutPolicy;
        this.threadNamePrefix = jobName.replaceAll("\\W+", "");
        this.executor = builder.executor != null
                ? builder.executor
                : Executors.newCachedThreadPool(new NamedDaemonThreadFactory(threadNamePrefix + "Job"));
        this.deadlineScheduler = Executors.newSingleThreadScheduledExecutor(new NamedDaemonThreadFactory(threadNamePrefix + "JobDeadline"));
        this.onJobRemoved = builder.onJobRemoved;
        this.onCleanup = builder.onCleanup;
        this.timeoutUnit = builder.timeoutUnit;
    }

    /**
     * @param jobName human-readable name of the job kind, used in logs and messages, for example "PDF conversion"
     */
    public static <P, R> @NotNull Builder<P, R> builder(@NotNull String jobName) {
        return new Builder<>(jobName);
    }

    public @NotNull String getJobName() {
        return jobName;
    }

    public @NotNull TimeoutPolicy getTimeoutPolicy() {
        return timeoutPolicy;
    }

    /**
     * Starts dropping the jobs that have been finished for longer than the given timeout. Does nothing if the cleaner already runs.
     *
     * @param finishedJobTimeoutInMinutes how long a finished job and its result are kept; the cleaner runs at the same interval
     */
    public synchronized void startCleaner(int finishedJobTimeoutInMinutes) {
        if (finishedJobTimeoutInMinutes <= 0) {
            throw new IllegalArgumentException("Finished job timeout must be positive: " + finishedJobTimeoutInMinutes);
        }
        if (cleanerService != null) {
            return;
        }
        cleanerService = Executors.newSingleThreadScheduledExecutor(new NamedDaemonThreadFactory(threadNamePrefix + "JobsCleaner"));
        cleanerService.scheduleWithFixedDelay(
                () -> {
                    // an exception thrown here would cancel all further runs of the cleaner
                    try {
                        cleanupExpiredJobs(finishedJobTimeoutInMinutes);
                    } catch (RuntimeException e) {
                        logger.error("Cleanup of %s jobs failed".formatted(jobName), e);
                    }
                },
                finishedJobTimeoutInMinutes,
                finishedJobTimeoutInMinutes,
                TimeUnit.MINUTES);
    }

    public synchronized void stopCleaner() {
        if (cleanerService != null) {
            cleanerService.shutdown();
            cleanerService = null;
        }
    }

    @VisibleForTesting
    public synchronized boolean isCleanerRunning() {
        return cleanerService != null;
    }

    /**
     * Drops the jobs that have been finished for longer than the given timeout. The timeout counts from the end of the job,
     * so a job that ran long is not dropped before its caller could read the result.
     */
    public void cleanupExpiredJobs(int finishedJobTimeoutInMinutes) {
        Instant currentTime = Instant.now();
        List<String> expiredJobIds = jobs.entrySet().stream()
                .filter(entry -> entry.getValue().isExpired(finishedJobTimeoutInMinutes, currentTime))
                .map(Map.Entry::getKey)
                .toList();
        expiredJobIds.forEach(this::removeJob);
        onCleanup.accept(finishedJobTimeoutInMinutes);
    }

    /**
     * Stops the cleaner and the threads of this registry. Call it when the extension stops.
     */
    public synchronized void shutdown() {
        stopCleaner();
        executor.shutdownNow();
        deadlineScheduler.shutdownNow();
    }

    /**
     * Cancels and forgets all jobs. For tests, which share the registry of an extension.
     */
    @VisibleForTesting
    public void clear() {
        jobs.values().forEach(job -> job.getFuture().cancel(true));
        jobs.clear();
    }

    void register(@NotNull AsyncJob<P, R> job, @NotNull Runnable work) {
        jobs.put(job.jobId(), job);
        try {
            executor.execute(work);
        } catch (RejectedExecutionException e) {
            jobs.remove(job.jobId());
            throw e;
        }
    }

    @NotNull ScheduledFuture<?> scheduleDeadline(@NotNull Runnable action, int timeoutInMinutes) {
        return deadlineScheduler.schedule(action, timeoutInMinutes, timeoutUnit);
    }

    @Nullable AsyncJob<P, R> getJob(@NotNull String jobId) {
        return jobs.get(jobId);
    }

    @NotNull Collection<AsyncJob<P, R>> getJobs() {
        return jobs.values();
    }

    private void removeJob(@NotNull String jobId) {
        jobs.remove(jobId);
        onJobRemoved.accept(jobId);
    }

    public static final class Builder<P, R> {
        private final @NotNull String jobName;
        private @NotNull TimeoutPolicy timeoutPolicy = TimeoutPolicy.INTERRUPT;
        private @Nullable ExecutorService executor;
        private @NotNull Consumer<String> onJobRemoved = jobId -> { };
        private @NotNull IntConsumer onCleanup = timeout -> { };
        private @NotNull TimeUnit timeoutUnit = TimeUnit.MINUTES;

        private Builder(@NotNull String jobName) {
            this.jobName = jobName;
        }

        public @NotNull Builder<P, R> timeoutPolicy(@NotNull TimeoutPolicy timeoutPolicy) {
            this.timeoutPolicy = timeoutPolicy;
            return this;
        }

        /**
         * The executor that runs the jobs. Default: an unbounded pool of daemon threads.
         * A bounded executor refuses jobs beyond its bounds: {@link AsyncJobsService#startJob} then throws {@link RejectedExecutionException}.
         */
        public @NotNull Builder<P, R> executor(@NotNull ExecutorService executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Called with the ID of each job the cleaner drops, for example to drop data stored elsewhere for that job.
         */
        public @NotNull Builder<P, R> onJobRemoved(@NotNull Consumer<String> onJobRemoved) {
            this.onJobRemoved = onJobRemoved;
            return this;
        }

        /**
         * Called with the finished job timeout after each cleanup run.
         */
        public @NotNull Builder<P, R> onCleanup(@NotNull IntConsumer onCleanup) {
            this.onCleanup = onCleanup;
            return this;
        }

        /**
         * The unit of the job timeout. Default: minutes. Tests set a shorter unit to reach a timeout quickly.
         */
        @VisibleForTesting
        public @NotNull Builder<P, R> timeoutUnit(@NotNull TimeUnit timeoutUnit) {
            this.timeoutUnit = timeoutUnit;
            return this;
        }

        public @NotNull JobsRegistry<P, R> build() {
            return new JobsRegistry<>(this);
        }
    }
}
