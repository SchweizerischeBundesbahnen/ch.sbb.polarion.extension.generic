package ch.sbb.polarion.extension.generic.jobs;

import ch.sbb.polarion.extension.generic.rest.model.jobs.JobStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncJobTest {

    /**
     * A job ends once: whoever ends it second finds it over, and the job keeps the status and the message of the first.
     */
    @Test
    void shouldEndOnceWithTheOutcomeOfWhoEndedItFirst() {
        AsyncJob<String, String> job = new AsyncJob<>("id", "user", null);

        assertThat(job.finish(JobOutcome.succeeded("result"))).isTrue();
        assertThat(job.finish(JobOutcome.cancelled(JobMessages.CANCELLED_BY_USER))).isFalse();

        JobState jobState = job.toJobState();
        assertThat(jobState.status()).isEqualTo(JobStatus.SUCCESSFULLY_FINISHED);
        assertThat(jobState.errorMessage()).isNull();
        assertThat(job.completion()).isDone();
    }

    @Test
    void shouldReportErrorOfFailedJob() {
        AsyncJob<String, String> job = new AsyncJob<>("id", "user", null);

        job.finish(JobOutcome.failed("broken"));

        JobState jobState = job.toJobState();
        assertThat(jobState.status()).isEqualTo(JobStatus.FAILED);
        assertThat(jobState.errorMessage()).isEqualTo("broken");
    }

    /**
     * A cooperative job asked to stop is still running, and says why it is stopping.
     */
    @Test
    void shouldReportPendingStopOfRunningJob() {
        AsyncJob<String, String> job = new AsyncJob<>("id", "user", null);

        job.requestStop(StopRequest.cancel());
        job.requestStop(StopRequest.timeout(5));

        JobState jobState = job.toJobState();
        assertThat(jobState.status()).isEqualTo(JobStatus.IN_PROGRESS);
        assertThat(jobState.errorMessage()).isEqualTo(JobMessages.CANCELLED_BY_USER);
        assertThat(job.isAbortRequested()).isTrue();
    }

    @Test
    void shouldTurnStopRequestsIntoTheirOutcomes() {
        assertThat(StopRequest.cancel().<String>toOutcome().status()).isEqualTo(JobStatus.CANCELLED);
        assertThat(StopRequest.shutdown().<String>toOutcome().status()).isEqualTo(JobStatus.CANCELLED);
        assertThat(StopRequest.timeout(5).<String>toOutcome())
                .isEqualTo(JobOutcome.failed(JobMessages.timeout(5)));
    }

    /**
     * Exactly one of the parties which may end a job takes it over, so its session is ended once.
     */
    @Test
    void shouldBeClaimedOnce() {
        AsyncJob<String, String> job = new AsyncJob<>("id", "user", null);

        assertThat(job.claim()).isTrue();
        assertThat(job.claim()).isFalse();
    }
}
