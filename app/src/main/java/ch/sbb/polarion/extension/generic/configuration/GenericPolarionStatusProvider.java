package ch.sbb.polarion.extension.generic.configuration;

import ch.sbb.polarion.extension.generic.util.VersionUtils;
import com.polarion.core.config.Configuration;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused") // Referenced in extensions which inherits from this project
public class GenericPolarionStatusProvider extends ConfigurationStatusProvider {
    public static final String POLARION_ALM = "Polarion ALM";

    @Override
    public @NotNull ConfigurationStatus getStatus(@NotNull Context context) {
        String currentCompatiblePolarionVersion = VersionUtils.getCurrentCompatiblePolarionVersion();
        if (currentCompatiblePolarionVersion == null || currentCompatiblePolarionVersion.trim().isEmpty()) {
            return new ConfigurationStatus(POLARION_ALM, Status.ERROR, "Officially supported version not specified");
        }

        String versionName = Configuration.getInstance().getProduct().versionName();
        if (versionName.startsWith(currentCompatiblePolarionVersion)) {
            return new ConfigurationStatus(POLARION_ALM, Status.OK, versionName);
        } else {
            return new ConfigurationStatus(POLARION_ALM, Status.WARNING, "%s is not officially supported".formatted(versionName));
        }
    }
}
