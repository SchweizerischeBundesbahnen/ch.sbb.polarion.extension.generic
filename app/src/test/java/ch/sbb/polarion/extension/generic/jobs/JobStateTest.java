package ch.sbb.polarion.extension.generic.jobs;

import ch.sbb.polarion.extension.generic.rest.model.jobs.JobDetails;
import ch.sbb.polarion.extension.generic.rest.model.jobs.JobStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class JobStateTest {

    @ParameterizedTest
    @CsvSource({
            "false, false, false, IN_PROGRESS",
            "true, false, false, SUCCESSFULLY_FINISHED",
            "true, true, false, FAILED",
            "true, true, true, CANCELLED",
    })
    void shouldDeriveStatus(boolean isDone, boolean isCompletedExceptionally, boolean isCancelled, JobStatus expectedStatus) {
        JobState jobState = new JobState(isDone, isCompletedExceptionally, isCancelled, "progress", "error");

        assertThat(jobState.status()).isEqualTo(expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({
            "false, false, progress",
            "true, false, ",
            "true, true, ",
    })
    void shouldKeepProgressOnlyWhileRunning(boolean isDone, boolean isCompletedExceptionally, String expectedProgress) {
        JobState jobState = new JobState(isDone, isCompletedExceptionally, false, "progress", "error");

        JobDetails jobDetails = JobDetails.from(jobState);

        assertThat(jobDetails.getStatus()).isEqualTo(jobState.status());
        assertThat(jobDetails.getProgressMessage()).isEqualTo(expectedProgress);
        assertThat(jobDetails.getErrorMessage()).isEqualTo("error");
    }
}
