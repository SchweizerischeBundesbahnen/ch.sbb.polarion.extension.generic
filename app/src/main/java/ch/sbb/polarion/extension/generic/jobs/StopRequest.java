package ch.sbb.polarion.extension.generic.jobs;

import ch.sbb.polarion.extension.generic.rest.model.jobs.JobStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Why a job is asked to stop, and how it ends if it does: a timeout makes it failed, a cancel or a shutdown makes it
 * cancelled.
 */
record StopRequest(@NotNull JobStatus status, @NotNull String message) {

    static @NotNull StopRequest cancel() {
        return new StopRequest(JobStatus.CANCELLED, JobMessages.CANCELLED_BY_USER);
    }

    static @NotNull StopRequest timeout(int timeoutInMinutes) {
        return new StopRequest(JobStatus.FAILED, JobMessages.timeout(timeoutInMinutes));
    }

    static @NotNull StopRequest shutdown() {
        return new StopRequest(JobStatus.CANCELLED, JobMessages.STOPPED);
    }

    <R> @NotNull JobOutcome<R> toOutcome() {
        return new JobOutcome<>(status, null, message);
    }
}
