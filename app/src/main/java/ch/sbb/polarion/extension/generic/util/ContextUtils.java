package ch.sbb.polarion.extension.generic.util;

import java.util.jar.Attributes;

import org.jetbrains.annotations.NotNull;

import ch.sbb.polarion.extension.generic.rest.model.Context;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ContextUtils {

    public static final String EXTENSION_CONTEXT = "Extension-Context";

    @NotNull
    public static Context getContext() {
        final Attributes attributes = ManifestUtils.getManifestAttributes();
        String extensionContext = attributes.getValue(EXTENSION_CONTEXT);
        return new Context(extensionContext);
    }
}
