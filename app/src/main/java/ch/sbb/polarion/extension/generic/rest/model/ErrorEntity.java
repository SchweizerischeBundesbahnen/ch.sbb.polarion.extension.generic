package ch.sbb.polarion.extension.generic.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Details about the error")
public class ErrorEntity {
    @Schema(description = "The error message", example = "Resource not found")
    private String message;
}
