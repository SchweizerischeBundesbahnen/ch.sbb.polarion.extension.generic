package ch.sbb.polarion.extension.generic.rest.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Version {
    private String bundleName;
    private String bundleVendor;
    private String supportEmail;
    private String automaticModuleName;
    private String bundleVersion;
    private String bundleBuildTimestamp;
    private String projectURL;

    public String getBundleBuildTimestampDigitsOnly() {
        return bundleBuildTimestamp.replaceAll("\\D", "");
    }
}
