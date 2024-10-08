package ch.sbb.polarion.extension.generic.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Details about the software version")
public class Version {

    @Schema(description = "The name of the bundle")
    private String bundleName;

    @Schema(description = "The vendor of the bundle")
    private String bundleVendor;

    @Schema(description = "Support email for the bundle")
    private String supportEmail;

    @Schema(description = "The automatic module name")
    private String automaticModuleName;

    @Schema(description = "The version of the bundle")
    private String bundleVersion;

    @Schema(description = "The build timestamp of the bundle")
    private String bundleBuildTimestamp;

    @Schema(description = "The project URL")
    private String projectURL;

    @Schema(description = "The build timestamp with only digits", hidden = true)
    public String getBundleBuildTimestampDigitsOnly() {
        return bundleBuildTimestamp.replaceAll("\\D", "");
    }
}
