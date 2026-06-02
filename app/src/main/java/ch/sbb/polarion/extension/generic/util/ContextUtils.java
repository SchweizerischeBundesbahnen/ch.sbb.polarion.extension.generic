package ch.sbb.polarion.extension.generic.util;

import ch.sbb.polarion.extension.generic.rest.model.Context;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
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
    private static List<Class<?>> discoverableClasses;

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
        return getDiscoverableClasses().stream()
                .filter(c -> type.isAssignableFrom(c) && !c.equals(type))
                .map(c -> c.asSubclass(type))
                .collect(Collectors.toSet());
    }

    private static synchronized List<Class<?>> getDiscoverableClasses() {
        if (discoverableClasses == null) {
            try (ScanResult result = new ClassGraph()
                    .acceptPackages(BASE_PACKAGE)
                    .enableClassInfo()
                    .enableAnnotationInfo()
                    .scan()) {
                discoverableClasses = result.getClassesWithAnnotation(Discoverable.class.getName()).loadClasses();
            }
        }
        return discoverableClasses;
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
