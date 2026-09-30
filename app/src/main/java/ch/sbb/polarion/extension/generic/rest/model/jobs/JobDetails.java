package ch.sbb.polarion.extension.generic.rest.model.jobs;

import ch.sbb.polarion.extension.generic.jobs.JobState;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Details of an asynchronous job: its status, its progress and the error message if it failed")
public class JobDetails {

    @Schema(description = "Current status of the job",
            example = "IN_PROGRESS",
            implementation = JobStatus.class
    )
    private JobStatus status;

    @Schema(description = "What the job is doing right now, as long as it is running")
    private String progressMessage;

    @Schema(description = "Error message if the job failed or was cancelled")
    private String errorMessage;

    /**
     * @return the details of the given job state. The progress message is kept only while the job runs.
     */
    public static @NotNull JobDetails from(@NotNull JobState jobState) {
        JobStatus status = jobState.status();
        return JobDetails.builder()
                .status(status)
                .progressMessage(status == JobStatus.IN_PROGRESS ? jobState.progressMessage() : null)
                .errorMessage(jobState.errorMessage())
                .build();
    }
}
