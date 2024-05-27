package ch.sbb.polarion.extension.generic.settings;

import java.util.Objects;

/**
 * Settings can be queried/processed using either NAME or ID (fileName).
 */
public final class SettingId {

    /**
     * ID or NAME depending on {@link SettingId#useName}
     */
    final String identifier;

    final boolean useName;

    private SettingId(String identifier, boolean useName) {
        this.identifier = identifier;
        this.useName = useName;
    }

    public static SettingId fromName(String name) {
        return new SettingId(name, true);
    }

    public static SettingId fromId(String id) {
        return new SettingId(id, false);
    }

    public String getIdentifier() {
        return identifier;
    }

    @SuppressWarnings("unused")
    public boolean isUseName() {
        return useName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SettingId settingId = (SettingId) o;
        return useName == settingId.useName && Objects.equals(identifier, settingId.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, useName);
    }
}
