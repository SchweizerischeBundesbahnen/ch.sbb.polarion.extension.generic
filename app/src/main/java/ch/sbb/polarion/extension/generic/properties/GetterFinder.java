package ch.sbb.polarion.extension.generic.properties;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

@UtilityClass
public class GetterFinder {

    public static @Nullable <T extends ExtensionConfiguration> String getValue(@NotNull T extensionConfiguration, @NotNull String propertyName) {
        return invokeGetter(extensionConfiguration, propertyName, "");
    }

    public static @Nullable <T extends ExtensionConfiguration> String getDescription(@NotNull T extensionConfiguration, @NotNull String propertyName) {
        return invokeGetter(extensionConfiguration, propertyName, "Description");
    }

    public static @Nullable <T extends ExtensionConfiguration> String getDefaultValue(@NotNull T extensionConfiguration, @NotNull String propertyName) {
        return invokeGetter(extensionConfiguration, propertyName, "DefaultValue");
    }

    private static @Nullable <T extends ExtensionConfiguration> String invokeGetter(@NotNull T extensionConfiguration, @NotNull String propertyName, @NotNull String suffix) {
        String getterMethodName = toCamelCaseGetter(propertyName) + suffix;

        // try "get" prefix
        String result = invokeMethod(extensionConfiguration, "get" + getterMethodName);
        if (result != null) {
            return result;
        }

        // try "is" prefix for boolean types
        return invokeMethod(extensionConfiguration, "is" + getterMethodName);
    }

    @SuppressWarnings({"squid:S1166", "findbugs:REC_CATCH_EXCEPTION"}) // no need to log or rethrow exception by design, findbugs wrongly assumes that getClass().getMethod() won't throw exceptions
    private static @Nullable <T extends ExtensionConfiguration> String invokeMethod(@NotNull T extensionConfiguration, @NotNull String methodName) {
        try {
            Method getter = extensionConfiguration.getClass().getMethod(methodName);
            if (methodName.startsWith("is") && getter.getReturnType() != boolean.class) {
                return null;
            }
            return String.valueOf(getter.invoke(extensionConfiguration));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static @NotNull String toCamelCaseGetter(@NotNull String propertyName) {
        String[] parts = propertyName.split("\\.");

        StringBuilder methodNameBuilder = new StringBuilder();
        for (String part : parts) {
            methodNameBuilder.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
        }
        return methodNameBuilder.toString();
    }

}
