package ch.sbb.polarion.extension.generic.jobs;

import ch.sbb.polarion.extension.generic.rest.model.jobs.JobStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * How a job ended. Set once, by whoever ended the job first, so its status and its error message always belong
 * together.
 *
 * @param status       never {@link JobStatus#IN_PROGRESS}
 * @param result       the result of a successful job
 * @param errorMessage why a job failed or was cancelled
 */
record JobOutcome<R>(@NotNull JobStatus status, @Nullable R result, @Nullable String errorMessage) {

    static <R> @NotNull JobOutcome<R> succeeded(@NotNull R result) {
        return new JobOutcome<>(JobStatus.SUCCESSFULLY_FINISHED, result, null);
    }

    static <R> @NotNull JobOutcome<R> failed(@NotNull String errorMessage) {
        return new JobOutcome<>(JobStatus.FAILED, null, errorMessage);
    }

    static <R> @NotNull JobOutcome<R> cancelled(@NotNull String errorMessage) {
        return new JobOutcome<>(JobStatus.CANCELLED, null, errorMessage);
    }
}
