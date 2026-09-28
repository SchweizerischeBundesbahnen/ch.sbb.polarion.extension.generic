package ch.sbb.polarion.extension.generic.jobs;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * One job of a {@link JobsRegistry}: who started it, its result, and what it reports while it runs.
 */
@Getter
final class AsyncJob<P, R> implements JobControl {

    private final @NotNull String jobId;
    private final @Nullable String user;
    private final @Nullable P payload;
    private final @NotNull Instant startingTime = Instant.now();
    private final @NotNull CompletableFuture<R> future = new CompletableFuture<>();
    private final AtomicReference<Instant> finishTime = new AtomicReference<>();
    private final AtomicReference<String> progressMessage = new AtomicReference<>();
    private final AtomicReference<String> failureReason = new AtomicReference<>();
    private final AtomicBoolean abortRequested = new AtomicBoolean();
    private final AtomicBoolean cancelRequested = new AtomicBoolean();
    private @Nullable Thread workerThread;

    AsyncJob(@NotNull String jobId, @Nullable String user, @Nullable P payload) {
        this.jobId = jobId;
        this.user = user;
        this.payload = payload;
        future.whenComplete((result, thrown) -> finishTime.set(Instant.now()));
    }

    @Override
    public @NotNull String jobId() {
        return jobId;
    }

    @Override
    public boolean isAbortRequested() {
        return abortRequested.get();
    }

    @Override
    public void reportProgress(@NotNull String message) {
        progressMessage.set(message);
    }

    void requestAbort() {
        abortRequested.set(true);
    }

    /**
     * Keeps the first reason only: a timeout or a cancel is recorded before the worker fails because of it.
     */
    void recordFailure(@NotNull String reason) {
        failureReason.compareAndSet(null, reason);
    }

    /**
     * Takes back a reason recorded for a job that then finished on its own, for example a cancel that came too late.
     */
    void withdrawFailure(@NotNull String reason) {
        failureReason.compareAndSet(reason, null);
    }

    void requestCancel() {
        cancelRequested.set(true);
    }

    boolean isCancelRequested() {
        return cancelRequested.get();
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
        if (!future.isDone()) {
            return false;
        }
        // a job which is done but whose completion has not been recorded yet has just this moment finished
        Instant finished = finishTime.get();
        return finished != null && finished.plusSeconds(finishedJobTimeoutInMinutes * 60L).isBefore(currentTime);
    }

    @NotNull JobState toJobState() {
        return JobState.builder()
                .isDone(future.isDone())
                .isCompletedExceptionally(future.isCompletedExceptionally())
                .isCancelled(future.isCancelled())
                .progressMessage(progressMessage.get())
                .errorMessage(failureReason.get())
                .build();
    }
}
