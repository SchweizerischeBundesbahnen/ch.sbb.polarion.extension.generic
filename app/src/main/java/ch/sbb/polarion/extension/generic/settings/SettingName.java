package ch.sbb.polarion.extension.generic.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Objects;

@Data
@Builder
public class SettingName {

    @JsonIgnore
    @Schema(hidden = true)
    private String id;

    @Schema(description = "The name of the setting")
    private String name;

    @Schema(description = "The scope of the setting")
    private String scope;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SettingName that = (SettingName) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
