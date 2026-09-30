package ch.sbb.polarion.extension.generic.rest;

import ch.sbb.polarion.extension.generic.rest.model.jobs.JobDetails;
import ch.sbb.polarion.extension.generic.rest.model.jobs.JobStatus;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

class JobResponsesTest {

    @ParameterizedTest
    @CsvSource({
            "IN_PROGRESS, 202, ",
            "SUCCESSFULLY_FINISHED, 303, /polarion/test/rest/jobs/id/result",
            "FAILED, 409, ",
            "CANCELLED, 409, ",
    })
    void shouldAnswerByStatus(JobStatus status, int expectedStatusCode, String expectedLocation) {
        UriInfo uriInfo = mock(UriInfo.class);
        lenient().when(uriInfo.getRequestUri()).thenReturn(URI.create("https://localhost/polarion/test/rest/jobs/id"));
        JobDetails jobDetails = JobDetails.builder().status(status).build();

        try (Response response = JobResponses.jobStatus(jobDetails, uriInfo)) {
            assertThat(response.getStatus()).isEqualTo(expectedStatusCode);
            assertThat(response.getEntity()).isSameAs(jobDetails);
            assertThat(response.getLocation() == null ? null : response.getLocation().toString()).isEqualTo(expectedLocation);
        }
    }
}
