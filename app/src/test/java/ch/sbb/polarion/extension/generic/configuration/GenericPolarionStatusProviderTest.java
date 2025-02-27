package ch.sbb.polarion.extension.generic.configuration;

import ch.sbb.polarion.extension.generic.util.VersionUtils;
import com.polarion.core.config.Configuration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

public class GenericPolarionStatusProviderTest {
    public static Stream<Arguments> provideConfigurationStatus() {
        return Stream.of(
                Arguments.of("2410", "2410", configurationStatus(Status.OK, "2410")),
                Arguments.of("2410.1", "2410", configurationStatus(Status.OK, "2410.1")),
                Arguments.of("2310", "2410", configurationStatus(Status.WARNING, "2310 is not officially supported")),
                Arguments.of("2310.1", "2410", configurationStatus(Status.WARNING, "2310.1 is not officially supported")),
                Arguments.of("2410", "", configurationStatus(Status.ERROR, "Officially supported version not specified")),
                Arguments.of("2410", null, configurationStatus(Status.ERROR, "Officially supported version not specified"))
        );
    }

    private static ConfigurationStatus configurationStatus(Status status, String details) {
        return ConfigurationStatus.builder()
                .name(GenericPolarionStatusProvider.POLARION_ALM)
                .status(status)
                .details(details)
                .build();
    }

    @ParameterizedTest
    @MethodSource("provideConfigurationStatus")
    void testConfigurationStatus(String polarionVersion, String currentCompatibleVersionPolarion, ConfigurationStatus expectedConfigurationStatus) {
        GenericPolarionStatusProvider polarionStatusProvider = new GenericPolarionStatusProvider();

        try (
                MockedStatic<Configuration> configurationMockedStatic = mockStatic(Configuration.class, Mockito.RETURNS_DEEP_STUBS);
                MockedStatic<VersionUtils> versionUtilsMockedStatic = mockStatic(VersionUtils.class)
        ) {
            configurationMockedStatic.when(() -> Configuration.getInstance().getProduct().versionName()).thenReturn(polarionVersion);
            versionUtilsMockedStatic.when(VersionUtils::getCurrentCompatiblePolarionVersion).thenReturn(currentCompatibleVersionPolarion);

            ConfigurationStatusProvider.Context context = ConfigurationStatusProvider.Context.builder().scope("").build();
            ConfigurationStatus configurationStatus = polarionStatusProvider.getStatus(context);
            assertEquals(expectedConfigurationStatus, configurationStatus);
        }
    }
}
