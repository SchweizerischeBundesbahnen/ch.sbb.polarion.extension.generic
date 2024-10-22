package ch.sbb.polarion.extension.generic.properties;

import lombok.experimental.UtilityClass;

import java.util.Properties;

@UtilityClass
public class SystemProperties {
    public static Properties getProperties() {
        return System.getProperties();
    }
}
