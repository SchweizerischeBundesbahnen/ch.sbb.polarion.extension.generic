package ch.sbb.polarion.extension.generic.util;

import com.polarion.alm.tracker.model.IPriorityOpt;
import com.polarion.platform.persistence.IEnumOption;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@UtilityClass
public class EnumUtils {

    /**
     * For some options (e.g. 'priority') method getId() returns weird float-like containing strings ("90.0", "50.0" etc.) instead if proper ID.
     * Was noticed that proper ID can be accessed from option properties by special key 'standardOptionId'.
     */
    public String getEnumId(@NotNull IEnumOption option) {
        return Optional.ofNullable(option.getProperty(IPriorityOpt.PROPERTY_KEY_STD_OPTION_ID)).orElse(option.getId());
    }

    public String getIconUrl(@NotNull IEnumOption option) {
        return option.getProperties() != null ? option.getProperties().getProperty("iconURL") : null;
    }
}
