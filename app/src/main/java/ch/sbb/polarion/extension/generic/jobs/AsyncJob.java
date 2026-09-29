package ch.sbb.polarion.extension.generic.jobs;

import ch.sbb.polarion.extension.generic.rest.model.jobs.JobStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * One job of a {@link JobsRegistry}: who started it, how it ended, and what it reports while it runs.
 * <p>
 * A job ends once: whoever ends it first - its worker, its deadline, a cancel or a shutdown - sets its
 * {@link JobOutcome}, and everybody else finds it over. Its status and its error message come from that outcome only.
 */
final class AsyncJob<P, R> implements JobControl {

    private final @NotNull String jobId;
    private final @Nullable String user;
    private final @Nullable P payload;
    private final AtomicReference<JobOutcome<R>> outcome = new AtomicReference<>();
    private final CompletableFuture<Void> completion = new CompletableFuture<>();
    private final AtomicReference<Instant> finishTime = new AtomicReference<>();
    private final AtomicReference<String> progressMessage = new AtomicReference<>();
    private final AtomicReference<StopRequest> stopRequest = new AtomicReference<>();
    private final AtomicBoolean claimed = new AtomicBoolean();
    private volatile @NotNull Runnable sessionRelease = () -> { };
    private volatile @Nullable Runnable work;
    private @Nullable Thread workerThread;

    AsyncJob(@NotNull String jobId, @Nullable String user, @Nullable P payload) {
        this.jobId = jobId;
        this.user = user;
        this.payload = payload;
    }

    @Override
    public @NotNull String jobId() {
        return jobId;
    }

    @Override
    public boolean isAbortRequested() {
        return stopRequest.get() != null;
    }

    @Override
    public void reportProgress(@NotNull String message) {
        progressMessage.set(message);
    }

    @Nullable String getUser() {
        return user;
    }

    @Nullable P getPayload() {
        return payload;
    }

    /**
     * Ends the job, unless it has already ended.
     *
     * @return {@code true} if this call ended it
     */
    boolean finish(@NotNull JobOutcome<R> jobOutcome) {
        if (!outcome.compareAndSet(null, jobOutcome)) {
            return false;
        }
        finishTime.set(Instant.now());
        completion.complete(null);
        return true;
    }

    boolean isOver() {
        return outcome.get() != null;
    }

    @Nullable JobOutcome<R> getOutcome() {
        return outcome.get();
    }

    /**
     * Completes when the job is over, whichever way it ended. For callbacks and for tests which wait for the end.
     */
    @NotNull CompletableFuture<Void> completion() {
        return completion;
    }

    /**
     * Asks the job to stop. The first request is kept: it is the reason a job which then stops reports.
     */
    void requestStop(@NotNull StopRequest request) {
        stopRequest.compareAndSet(null, request);
    }

    @Nullable StopRequest getStopRequest() {
        return stopRequest.get();
    }

    /**
     * Takes the job over, for the worker which runs it, or for whoever drops it before it ran. Exactly one of them
     * wins, and it is the one which ends the session kept for the job.
     *
     * @return {@code true} for the caller which took the job over
     */
    boolean claim() {
        return claimed.compareAndSet(false, true);
    }

    /**
     * Sets what ends the session kept alive for this job, if one was.
     */
    void setSessionRelease(@NotNull Runnable sessionRelease) {
        this.sessionRelease = sessionRelease;
    }

    void releaseSession() {
        sessionRelease.run();
    }

    /**
     * The runnable handed to the executor, so that a job which ends while it still waits can be taken out of the
     * executor's queue.
     */
    void setWork(@NotNull Runnable work) {
        this.work = work;
    }

    @Nullable Runnable getWork() {
        return work;
    }

    synchronized void attachWorker(@NotNull Thread thread) {
        workerThread = thread;
    }

    /**
     * Detached under the same lock as {@link #interruptWorker()}, so an interrupt never reaches the next job of this thread.
     */
    synchronized void detachWorker() {
        workerThread = null;
    }

    synchronized void interruptWorker() {
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }

    boolean isExpired(int finishedJobTimeoutInMinutes, @NotNull Instant currentTime) {
        Instant finished = finishTime.get();
        return finished != null && finished.plusSeconds(finishedJobTimeoutInMinutes * 60L).isBefore(currentTime);
    }

    /**
     * A running job reports why it was asked to stop, if it was: a cooperative job stops at its next safe point.
     */
    @NotNull JobState toJobState() {
        JobOutcome<R> jobOutcome = outcome.get();
        if (jobOutcome == null) {
            StopRequest stop = stopRequest.get();
            return new JobState(false, false, false, progressMessage.get(), stop == null ? null : stop.message());
        }
        return new JobState(true,
                jobOutcome.status() != JobStatus.SUCCESSFULLY_FINISHED,
                jobOutcome.status() == JobStatus.CANCELLED,
                progressMessage.get(),
                jobOutcome.errorMessage());
    }
}
