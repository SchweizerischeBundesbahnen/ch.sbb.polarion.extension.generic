package ch.sbb.polarion.extension.generic.rest.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Version {
    private String bundleName;
    private String bundleVendor;
    private String supportEmail;
    private String automaticModuleName;
    private String bundleVersion;
    private String bundleBuildTimestamp;

    public String getBundleBuildTimestampDigitsOnly() {
        return bundleBuildTimestamp.replaceAll("\\D", "");
    }
}
