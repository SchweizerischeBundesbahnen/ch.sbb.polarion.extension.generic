package ch.sbb.polarion.extension.generic.properties;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

@UtilityClass
public class GetterFinder {

    @SuppressWarnings({"squid:S1166", "findbugs:REC_CATCH_EXCEPTION"}) // no need to log or rethrow exception by design, findbugs wrongly assumes that getClass().getMethod() won't throw exceptions
    public static @Nullable <T extends ExtensionConfiguration> String getValue(@NotNull T extensionConfiguration, @NotNull String propertyName) {
        String getterMethodName = toCamelCaseGetter(propertyName);

        try {
            Method getter = extensionConfiguration.getClass().getMethod("get" + getterMethodName);
            return String.valueOf(getter.invoke(extensionConfiguration));
        } catch (Exception ignored) {
            //ignore
        }

        try {
            Method getter = extensionConfiguration.getClass().getMethod("is" + getterMethodName);
            if (getter.getReturnType() == boolean.class) {
                return String.valueOf(getter.invoke(extensionConfiguration));
            }
        } catch (Exception ignored) {
            //ignore
        }

        return null;
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
