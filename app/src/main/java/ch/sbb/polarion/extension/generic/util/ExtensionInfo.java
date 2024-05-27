package ch.sbb.polarion.extension.generic.util;

import ch.sbb.polarion.extension.generic.rest.model.Context;
import ch.sbb.polarion.extension.generic.rest.model.Version;
import lombok.Getter;

@Getter
public final class ExtensionInfo {

    private Version version;
    private Context context;

    private ExtensionInfo() {
        version = VersionUtils.getVersion();
        context = ContextUtils.getContext();
    }

    public static ExtensionInfo getInstance() {
        return ExtensionInfoHolder.INSTANCE;
    }

    private static class ExtensionInfoHolder {
        private static final ExtensionInfo INSTANCE = new ExtensionInfo();
    }
}
