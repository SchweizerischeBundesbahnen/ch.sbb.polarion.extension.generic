package ch.sbb.polarion.extension.generic.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.jar.Attributes;

import org.jetbrains.annotations.NotNull;

import ch.sbb.polarion.extension.generic.rest.model.Version;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class VersionUtils {

    private static final String GENERIC_PROPERTIES_FILE = "src/main/resources-filtered/generic.properties";

    public static final String BUNDLE_NAME = "Bundle-Name";
    public static final String BUNDLE_VENDOR = "Bundle-Vendor";
    public static final String SUPPORT_EMAIL = "Support-Email";
    public static final String AUTOMATIC_MODULE_NAME = "Automatic-Module-Name";
    public static final String BUNDLE_VERSION = "Bundle-Version";
    public static final String BUNDLE_BUILD_TIMESTAMP = "Bundle-Build-Timestamp";
    public static final String PROJECT_URL = "Project-URL";

    @NotNull
    public static Version getVersion() {
        final Attributes attributes = ManifestUtils.getManifestAttributes();

        String bundleName = attributes.getValue(BUNDLE_NAME);
        String bundleVendor = attributes.getValue(BUNDLE_VENDOR);
        String supportEmail = attributes.getValue(SUPPORT_EMAIL);
        String automaticModuleName = attributes.getValue(AUTOMATIC_MODULE_NAME);
        String bundleVersion = attributes.getValue(BUNDLE_VERSION);
        String bundleBuildTimestamp = attributes.getValue(BUNDLE_BUILD_TIMESTAMP);
        String projectURL = attributes.getValue(PROJECT_URL);

        return Version.builder()
                .bundleName(bundleName)
                .bundleVendor(bundleVendor)
                .supportEmail(supportEmail)
                .automaticModuleName(automaticModuleName)
                .bundleVersion(bundleVersion)
                .bundleBuildTimestamp(bundleBuildTimestamp)
                .projectURL(projectURL)
                .build();
    }

    public static @Nullable String getCurrentCompatiblePolarionVersion() {
        return getVersionFromProperties("polarion.version");
    }

    public static @Nullable String getVersionFromProperties(String key) {
        return getVersionFromProperties(GENERIC_PROPERTIES_FILE, key);
    }

    public static @Nullable String getVersionFromProperties(String propertiesFileName, String key) {
        try (InputStream input = VersionUtils.class.getClassLoader().getResourceAsStream(propertiesFileName)) {
            if (input == null) {
                return null;
            }

            Properties properties = new Properties();
            properties.load(input);
            return properties.getProperty(key);
        } catch (IOException e) {
            return null;
        }
    }
}
