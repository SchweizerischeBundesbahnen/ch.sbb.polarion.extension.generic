package ch.sbb.polarion.extension.generic.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Extension configuration properties shown on the About page")
public class ConfigurationPropertiesModel {

    @Schema(description = "Active configuration properties, sorted by key")
    private List<ConfigurationPropertyModel> properties;

    @Schema(description = "Obsolete or non-valid configuration properties (only key and value are set), sorted by key")
    private List<ConfigurationPropertyModel> obsoleteProperties;
}
