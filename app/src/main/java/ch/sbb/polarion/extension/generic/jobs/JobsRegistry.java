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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
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
    private final @NotNull ThreadPoolExecutor executor;
    private final @NotNull ScheduledExecutorService deadlineScheduler;
    private final @NotNull Consumer<String> onJobRemoved;
    private final @NotNull IntConsumer onCleanup;
    private final @NotNull TimeUnit timeoutUnit;
    private @Nullable ScheduledExecutorService cleanerService;
    private boolean shutDown;

    private JobsRegistry(@NotNull Builder<P, R> builder) {
        this.jobName = builder.jobName;
        this.timeoutPolicy = builder.timeoutPolicy;
        this.threadNamePrefix = jobName.replaceAll("\\W+", "");
        this.executor = createExecutor(builder, new NamedDaemonThreadFactory(threadNamePrefix + "Job"));
        this.deadlineScheduler = Executors.newSingleThreadScheduledExecutor(new NamedDaemonThreadFactory(threadNamePrefix + "JobDeadline"));
        this.onJobRemoved = builder.onJobRemoved;
        this.onCleanup = builder.onCleanup;
        this.timeoutUnit = builder.timeoutUnit;
    }

    /**
     * Always a {@link ThreadPoolExecutor} owned by this registry, handed the job's own runnable, so that a job which
     * ends while it waits can be taken back out of its queue.
     */
    private static @NotNull ThreadPoolExecutor createExecutor(@NotNull Builder<?, ?> builder, @NotNull ThreadFactory threadFactory) {
        if (builder.maxRunningJobs == null) {
            // what Executors.newCachedThreadPool builds: a thread for each job which finds none idle
            return new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), threadFactory);
        }
        return new ThreadPoolExecutor(builder.maxRunningJobs, builder.maxRunningJobs, 0L, TimeUnit.MILLISECONDS,
                builder.maxQueuedJobs == 0 ? new SynchronousQueue<>() : new ArrayBlockingQueue<>(builder.maxQueuedJobs),
                threadFactory);
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
        if (shutDown) {
            logger.warn("Cleaner of %s jobs is not started: the registry is shut down".formatted(jobName));
            return;
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
        cleanupExpiredJobs(finishedJobTimeoutInMinutes, Instant.now());
    }

    @VisibleForTesting
    void cleanupExpiredJobs(int finishedJobTimeoutInMinutes, @NotNull Instant currentTime) {
        List<String> expiredJobIds = jobs.entrySet().stream()
                .filter(entry -> entry.getValue().isExpired(finishedJobTimeoutInMinutes, currentTime))
                .map(Map.Entry::getKey)
                .toList();
        expiredJobIds.forEach(this::removeJob);
        try {
            onCleanup.accept(finishedJobTimeoutInMinutes);
        } catch (RuntimeException e) {
            logger.error("Cleanup hook of %s jobs failed".formatted(jobName), e);
        }
    }

    /**
     * Stops the cleaner and the threads of this registry, and ends the jobs which are not over. Call it when the
     * extension stops. Jobs asked for from then on are refused with {@link JobsRegistryShutDownException}.
     * <p>
     * A job still waiting for a thread has done nothing yet: it is cancelled and never runs, and the session kept for
     * it is ended here. A running job is treated as its {@link TimeoutPolicy} says for a stop:
     * <ul>
     *     <li>{@link TimeoutPolicy#INTERRUPT}: it is cancelled at once and its thread is interrupted;</li>
     *     <li>{@link TimeoutPolicy#COOPERATIVE}: it is asked to stop and its thread is <b>not</b> interrupted - it may
     *     be writing - so it stops at its next safe point, or finishes the write it is in, on its own thread.</li>
     * </ul>
     * Either way the job reports {@link JobMessages#STOPPED}, unless it was asked to stop before, for a cancel or its
     * deadline: the first request decides how a job ends.
     */
    public synchronized void shutdown() {
        shutDown = true;
        stopCleaner();
        StopRequest stop = StopRequest.shutdown();
        List<AsyncJob<P, R>> unfinishedJobs = jobs.values().stream().filter(job -> !job.isOver()).toList();
        unfinishedJobs.forEach(job -> job.requestStop(stop));
        if (timeoutPolicy == TimeoutPolicy.INTERRUPT) {
            unfinishedJobs.forEach(job -> job.finish(job.requestStop(stop).toOutcome()));
            // drops the jobs which wait, and interrupts the running ones
            executor.shutdownNow();
        } else {
            // the jobs which wait must not start; the running ones must not be interrupted
            executor.getQueue().clear();
            executor.shutdown();
        }
        deadlineScheduler.shutdownNow();
        // a job no worker has taken over never will: it was dropped, or its worker finds it taken over
        Throwable firstFailure = null;
        for (AsyncJob<P, R> job : unfinishedJobs) {
            if (job.claim()) {
                job.finish(job.requestStop(stop).toOutcome());
                firstFailure = releaseSession(job, firstFailure);
            }
        }
        if (firstFailure instanceof Error error) {
            throw error;
        }
    }

    /**
     * Ends the session of a job which shutdown took over. A failure is kept for after the last job, so that every job
     * is ended and every session given its chance, and an {@link Error} is not lost.
     *
     * @return the first failure of this shutdown, so far
     */
    @SuppressWarnings("java:S1181") // one session which cannot be ended must not keep the others open
    private @Nullable Throwable releaseSession(@NotNull AsyncJob<P, R> job, @Nullable Throwable firstFailure) {
        try {
            job.releaseSession();
            return firstFailure;
        } catch (Throwable e) {
            logger.error("Cannot end the session of %s job '%s'".formatted(jobName, job.jobId()), e);
            return firstFailure != null ? firstFailure : e;
        }
    }

    /**
     * Waits until the threads of this registry are done, after {@link #shutdown()}. For tests, which check what the
     * jobs did once nothing runs any more.
     *
     * @return {@code true} if they are done within the given time
     */
    @VisibleForTesting
    boolean awaitTermination(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit) && deadlineScheduler.awaitTermination(timeout, unit);
    }

    /**
     * Cancels and forgets all jobs. For tests, which share the registry of an extension.
     */
    @VisibleForTesting
    public void clear() {
        jobs.values().forEach(job -> job.finish(JobOutcome.cancelled("Cleared")));
        jobs.clear();
    }

    /**
     * Registers a job, hands its work to the executor and schedules its deadline, as one step with respect to
     * {@link #shutdown()}: under the same lock, so a job is either refused or fully started, with a deadline and in
     * the view of a shutdown which comes after it.
     *
     * @return the deadline, to be cancelled once the job is over
     * @throws RejectedExecutionException if the executor has no room for the job, or the registry is shut down
     */
    @SuppressWarnings( "java:S1452")
    synchronized @NotNull ScheduledFuture<?> submit(@NotNull AsyncJob<P, R> job, @NotNull Runnable work,
                                                    @NotNull Runnable onDeadline, int timeoutInMinutes) {
        if (shutDown) {
            throw new JobsRegistryShutDownException("%s jobs are not accepted: the extension is stopping".formatted(jobName));
        }
        job.setWork(work);
        jobs.put(job.jobId(), job);
        try {
            executor.execute(work);
        } catch (RejectedExecutionException e) {
            jobs.remove(job.jobId());
            throw e;
        }
        // the scheduler is alive while this lock is held and the registry is not shut down, so this cannot be refused
        return deadlineScheduler.schedule(onDeadline, timeoutInMinutes, timeoutUnit);
    }

    /**
     * Takes a job which still waits for a thread out of the executor's queue, so that it never runs and no longer
     * takes a place in a bounded queue. A job already handed to a thread is not in the queue: it finds itself stopped
     * when it starts, see {@code AsyncJobsService.runJob}.
     *
     * @return {@code true} if the job was taken back and taken over by the caller, which then ends it and its session
     */
    boolean takeBackIfQueued(@NotNull AsyncJob<P, R> job) {
        Runnable work = job.getWork();
        return work != null && executor.remove(work) && job.claim();
    }

    @Nullable AsyncJob<P, R> getJob(@NotNull String jobId) {
        return jobs.get(jobId);
    }

    @NotNull Collection<AsyncJob<P, R>> getJobs() {
        return jobs.values();
    }

    private void removeJob(@NotNull String jobId) {
        jobs.remove(jobId);
        // A failure here is the callback's to recover from, not a reason to keep the job: a job kept until its callback
        // succeeds would stay forever, with its result, if the callback keeps failing. It must neither stop the other
        // jobs of this run from being dropped nor the cleanup hook from running.
        try {
            onJobRemoved.accept(jobId);
        } catch (RuntimeException e) {
            logger.error("Removal hook of %s job '%s' failed".formatted(jobName, jobId), e);
        }
    }

    public static final class Builder<P, R> {
        private final @NotNull String jobName;
        private @NotNull TimeoutPolicy timeoutPolicy = TimeoutPolicy.INTERRUPT;
        private @Nullable Integer maxRunningJobs;
        private int maxQueuedJobs;
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
         * Bounds the jobs of this kind: at most {@code maxRunningJobs} run at once, at most {@code maxQueuedJobs} wait
         * for a thread, and a job beyond both is refused with {@link RejectedExecutionException}, which the extension
         * should answer with 429. A waiting job's timeout runs while it waits.
         * <p>
         * Without it, jobs are not bounded: a thread is started for every job which finds none idle. Jobs that wait on
         * external work, such as a conversion service, can then pile up threads for as long as callers keep starting
         * them. Either way the threads are daemon threads named after the job kind.
         *
         * @param maxRunningJobs at least 1
         * @param maxQueuedJobs  0 or more; 0 refuses a job as soon as all threads are busy
         */
        public @NotNull Builder<P, R> maxConcurrentJobs(int maxRunningJobs, int maxQueuedJobs) {
            if (maxRunningJobs < 1) {
                throw new IllegalArgumentException("At least one job must be able to run: " + maxRunningJobs);
            }
            if (maxQueuedJobs < 0) {
                throw new IllegalArgumentException("The number of queued jobs cannot be negative: " + maxQueuedJobs);
            }
            this.maxRunningJobs = maxRunningJobs;
            this.maxQueuedJobs = maxQueuedJobs;
            return this;
        }

        /**
         * Called with the ID of each job the cleaner drops, for example to drop data stored elsewhere for that job.
         * <p>
         * Called once per job, after the job is dropped: a failure is logged and not retried. Data which must go
         * whatever happens should also be swept by {@link #onCleanup(IntConsumer)}, which runs after every cleanup.
         */
        public @NotNull Builder<P, R> onJobRemoved(@NotNull Consumer<String> onJobRemoved) {
            this.onJobRemoved = onJobRemoved;
            return this;
        }

        /**
         * Called with the finished job timeout after each cleanup run, also when a removal hook of that run failed.
         * The place for a sweep which does not depend on a single job, for example dropping data older than the timeout.
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
