package ch.sbb.polarion.extension.generic.rest;

import ch.sbb.polarion.extension.generic.rest.model.jobs.JobDetails;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

/**
 * Responses of the REST endpoint that a caller of an asynchronous job polls.
 */
@UtilityClass
public class JobResponses {

    public static final String RESULT_PATH = "result";

    /**
     * @return 202 while the job runs; 303 with the {@code result} sub-path of the request as its location when the job
     * finished successfully; 409 when it failed or was cancelled. The entity is always the given job details.
     */
    public static @NotNull Response jobStatus(@NotNull JobDetails jobDetails, @NotNull UriInfo uriInfo) {
        Response.ResponseBuilder responseBuilder = switch (jobDetails.getStatus()) {
            case IN_PROGRESS -> Response.accepted();
            case SUCCESSFULLY_FINISHED -> Response.status(Response.Status.SEE_OTHER)
                    .location(UriBuilder.fromUri(uriInfo.getRequestUri().getPath()).path(RESULT_PATH).build());
            case FAILED, CANCELLED -> Response.status(Response.Status.CONFLICT);
        };
        return responseBuilder.entity(jobDetails).build();
    }
}
