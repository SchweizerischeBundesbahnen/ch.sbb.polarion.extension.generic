package ch.sbb.polarion.extension.generic.jobs;

import ch.sbb.polarion.extension.generic.rest.model.jobs.JobStatus;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Snapshot of an asynchronous job, as its caller polls it.
 *
 * @param isDone                   the job is over, whichever way it ended
 * @param isCompletedExceptionally the job failed or was cancelled
 * @param isCancelled              the job was cancelled
 * @param progressMessage          the last progress the job reported, if any
 * @param errorMessage             why the job failed or was cancelled, if it did
 */
@Builder
public record JobState(
        boolean isDone,
        boolean isCompletedExceptionally,
        boolean isCancelled,
        @Nullable String progressMessage,
        @Nullable String errorMessage) {

    public @NotNull JobStatus status() {
        if (!isDone) {
            return JobStatus.IN_PROGRESS;
        } else if (isCancelled) {
            return JobStatus.CANCELLED;
        } else if (isCompletedExceptionally) {
            return JobStatus.FAILED;
        } else {
            return JobStatus.SUCCESSFULLY_FINISHED;
        }
    }
}
