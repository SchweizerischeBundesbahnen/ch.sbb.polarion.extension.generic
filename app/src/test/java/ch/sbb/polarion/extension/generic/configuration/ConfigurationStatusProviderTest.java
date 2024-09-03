package ch.sbb.polarion.extension.generic.configuration;

import ch.sbb.polarion.extension.generic.util.ContextUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

class ConfigurationStatusProviderTest {

    @Test
    void testGetAllStatuses() {
        try (MockedStatic<ContextUtils> contextUtilsMockedStatic = mockStatic(ContextUtils.class)) {
            contextUtilsMockedStatic.when(() -> ContextUtils.findSubTypes(any())).thenReturn(Set.of(TestSingleProvider.class, TestMultipleProvider.class));
            Collection<ConfigurationStatus> statuses = ConfigurationStatusProvider.getAllStatuses("some scope");

            assertEquals(3, statuses.size());

            // Check alphabetical order of configurations
            ConfigurationStatus[] statusesArray = statuses.toArray(new ConfigurationStatus[0]);
            assertEquals(statusesArray[0], new ConfigurationStatus("multiple 1", Status.WARNING, "multiple 1 details"));
            assertEquals(statusesArray[1], new ConfigurationStatus("multiple 2", Status.ERROR, "multiple 2 details"));
            assertEquals(statusesArray[2], new ConfigurationStatus("single", Status.OK, "single details"));
        }
    }

    @Test
    void testGetConfigurationStatus() {
        assertEquals(new ConfigurationStatus("test ok", Status.OK, ""), new TestSingleProvider().getConfigurationStatus("test ok", "some content", "content"));
        assertEquals(new ConfigurationStatus("test warning", Status.WARNING, "Not configured"), new TestSingleProvider().getConfigurationStatus("test warning", "some content", "another text"));
    }

    public static class TestSingleProvider extends ConfigurationStatusProvider {
        @Override
        public @NotNull ConfigurationStatus getStatus(@NotNull Context context) {
            return new ConfigurationStatus("single", Status.OK, "single details");
        }
    }

    public static class TestMultipleProvider extends ConfigurationStatusProvider {
        @Override
        public @NotNull List<ConfigurationStatus> getStatuses(@NotNull Context context) {
            return List.of(
                    new ConfigurationStatus("multiple 1", Status.WARNING, "multiple 1 details"),
                    new ConfigurationStatus("multiple 2", Status.ERROR, "multiple 2 details")
            );
        }
    }
}