package ch.sbb.polarion.extension.generic.rest.model.jobs;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Status of an asynchronous job")
public enum JobStatus {

    @Schema(description = "The job is currently in progress")
    IN_PROGRESS,

    @Schema(description = "The job has finished successfully and produced a result")
    SUCCESSFULLY_FINISHED,

    @Schema(description = "The job has failed and produced no result")
    FAILED,

    @Schema(description = "The job was cancelled")
    CANCELLED
}
