package ch.sbb.polarion.extension.generic.util;

import java.util.jar.Attributes;

import org.jetbrains.annotations.NotNull;

import ch.sbb.polarion.extension.generic.rest.model.Version;
import lombok.experimental.UtilityClass;

@UtilityClass
public class VersionUtils {

    public static final String BUNDLE_NAME = "Bundle-Name";
    public static final String BUNDLE_VENDOR = "Bundle-Vendor";
    public static final String SUPPORT_EMAIL = "Support-Email";
    public static final String AUTOMATIC_MODULE_NAME = "Automatic-Module-Name";
    public static final String BUNDLE_VERSION = "Bundle-Version";
    public static final String BUNDLE_BUILD_TIMESTAMP = "Bundle-Build-Timestamp";

    @NotNull
    public static Version getVersion() {
        final Attributes attributes = ManifestUtils.getManifestAttributes();
        String bundleName = attributes.getValue(BUNDLE_NAME);
        String bundleVendor = attributes.getValue(BUNDLE_VENDOR);
        String supportEmail = attributes.getValue(SUPPORT_EMAIL);
        String automaticModuleName = attributes.getValue(AUTOMATIC_MODULE_NAME);
        String bundleVersion = attributes.getValue(BUNDLE_VERSION);
        String bundleBuildTimestamp = attributes.getValue(BUNDLE_BUILD_TIMESTAMP);
        return new Version(bundleName, bundleVendor, supportEmail, automaticModuleName, bundleVersion, bundleBuildTimestamp);
    }

}
