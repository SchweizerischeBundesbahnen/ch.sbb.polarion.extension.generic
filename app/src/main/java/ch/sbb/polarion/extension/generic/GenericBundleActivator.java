package ch.sbb.polarion.extension.generic;

import com.polarion.alm.ui.server.forms.extensions.IFormExtension;
import com.polarion.alm.ui.server.forms.extensions.impl.FormExtensionsRegistry;
import com.polarion.core.util.logging.Logger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.Map;

/**
 * Simplifies the way of registering form extension(s).
 */
@SuppressWarnings("unused")
public abstract class GenericBundleActivator implements BundleActivator {

    private static final Logger logger = Logger.getLogger(GenericBundleActivator.class);

    protected abstract Map<String, IFormExtension> getExtensions();

    public void onStart(BundleContext context) {
        // for overriding if needed
    }

    @Override
    public void start(BundleContext context) {
        onStart(context);
        getExtensions().forEach((key, value) -> {
            logger.info("Registering form extension: " + key);
            FormExtensionsRegistry.getInstance().registerExtension(key, value);
        });
    }

    @Override
    public void stop(BundleContext context) {
        // nothing by default
    }
}
