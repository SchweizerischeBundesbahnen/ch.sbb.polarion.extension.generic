package ch.sbb.polarion.extension.generic.configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationStatus implements Comparable<ConfigurationStatus> {
    private @NotNull String name;
    private @NotNull Status status;
    private @NotNull String details;

    public ConfigurationStatus(@NotNull String name, @NotNull Status status) {
        this(name, status, "");
    }

    @Override
    public int compareTo(@NotNull ConfigurationStatus o) {
        return name.compareTo(o.name);
    }
}
