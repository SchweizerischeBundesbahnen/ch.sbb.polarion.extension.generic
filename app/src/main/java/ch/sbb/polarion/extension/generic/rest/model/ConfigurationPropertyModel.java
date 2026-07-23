package ch.sbb.polarion.extension.generic.rest.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "A single extension configuration property as shown on the About page")
public class ConfigurationPropertyModel {

    @Schema(description = "Property key")
    private String key;

    @Schema(description = "Current value of the property")
    private String value;

    @Schema(description = "Default value of the property, if any")
    private String defaultValue;

    @Schema(description = "Human readable description of the property, if any")
    private String description;
}
