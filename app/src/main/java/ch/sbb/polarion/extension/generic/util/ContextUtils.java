package ch.sbb.polarion.extension.generic.util;

import ch.sbb.polarion.extension.generic.rest.model.Context;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.util.Optional;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.stream.Collectors;

@UtilityClass
public class ContextUtils {

    public static final String CH_SBB_POLARION_EXTENSION = "ch.sbb.polarion.extension.";

    public static final String EXTENSION_CONTEXT = "Extension-Context";
    public static final String DISCOVER_BASE_PACKAGE = "Discover-Base-Package";
    public static final String CONFIGURATION_PROPERTIES_PREFIX = "Configuration-Properties-Prefix";

    private static final String BASE_PACKAGE = getBasePackage();
    private static Reflections reflections;

    @NotNull
    public static Context getContext() {
        @NotNull Attributes attributes = ManifestUtils.getManifestAttributes();
        @Nullable String extensionContext = attributes.getValue(EXTENSION_CONTEXT);
        if (extensionContext == null || extensionContext.isBlank()) {
            throw new IllegalStateException("Extension context is not provided");
        }
        return new Context(extensionContext);
    }

    public <T> Set<Class<? extends T>> findSubTypes(Class<T> type) {
        return getReflections().getSubTypesOf(type).stream().filter(c -> c.isAnnotationPresent(Discoverable.class)).collect(Collectors.toSet());
    }

    private static Reflections getReflections() {
        if (reflections == null) {
            reflections = new Reflections(
                    new ConfigurationBuilder()
                            .setUrls(
                                    ClasspathHelper.forPackage(BASE_PACKAGE).stream()
                                            .filter(url -> "file".equals(url.getProtocol()))
                                            .toList()
                            )
                            .forPackage(BASE_PACKAGE)
            );
        }
        return reflections;
    }

    private static String getBasePackage() {
        return Optional.ofNullable(ManifestUtils.getManifestAttributes().getValue(DISCOVER_BASE_PACKAGE)).orElse(CH_SBB_POLARION_EXTENSION);
    }

    public static String getConfigurationPropertiesPrefix() {
        String configurationPropertiesPrefix = ManifestUtils.getManifestAttributes().getValue(CONFIGURATION_PROPERTIES_PREFIX);
        if (configurationPropertiesPrefix == null || configurationPropertiesPrefix.isBlank()) {
            configurationPropertiesPrefix = CH_SBB_POLARION_EXTENSION + ContextUtils.getContext().getExtensionContext() + ".";
        }
        return configurationPropertiesPrefix.endsWith(".") ? configurationPropertiesPrefix : configurationPropertiesPrefix + ".";
    }
}
