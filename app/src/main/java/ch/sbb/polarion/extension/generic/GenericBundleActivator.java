package ch.sbb.polarion.extension.generic;

import com.polarion.alm.ui.server.forms.extensions.IFormExtension;
import com.polarion.alm.ui.server.forms.extensions.impl.FormExtensionsRegistry;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.guice.internal.GuicePlatform;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Simplifies the way of registering form extension(s).
 * <p>
 * Registration is performed on a background thread and only once Polarion's global Guice injector
 * is available — see {@link #registerExtensionsWhenReady(Map)} for why this deferral is required.
 */
@SuppressWarnings("unused")
public abstract class GenericBundleActivator implements BundleActivator {

    private static final Logger logger = Logger.getLogger(GenericBundleActivator.class);

    /** How long to wait for Polarion's global Guice injector before registering anyway. */
    private static final long PLATFORM_WAIT_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(5);
    /** How often to poll for the global Guice injector while waiting. */
    private static final long PLATFORM_POLL_INTERVAL_MS = 200;

    /** {@code GuicePlatform.getGlobalInjector()}, resolved reflectively; {@code null} if unavailable. */
    private static final Method GET_GLOBAL_INJECTOR = resolveGetGlobalInjector();

    protected abstract Map<String, IFormExtension> getExtensions();

    protected void onStart(BundleContext context) {
        // for overriding if needed
    }

    @Override
    public void start(BundleContext context) {
        onStart(context);
        Map<String, IFormExtension> extensions = getExtensions();
        if (extensions.isEmpty()) {
            return;
        }
        // Do not touch FormExtensionsRegistry on the OSGi activation thread — defer it, see
        // registerExtensionsWhenReady(). start() must stay non-blocking: the global injector may
        // still be built later on this very thread, so blocking here could deadlock startup.
        runAsync(() -> registerExtensionsWhenReady(extensions));
    }

    @Override
    public void stop(BundleContext context) {
        // nothing by default
    }

    /**
     * Runs the deferred registration task. Spawns a daemon thread by default; overridable so tests
     * can execute it synchronously.
     */
    protected void runAsync(@NotNull Runnable task) {
        Thread registrar = new Thread(task, "generic-form-extension-registrar");
        registrar.setDaemon(true);
        registrar.start();
    }

    /**
     * Registers this bundle's form extensions, but only after Polarion's global Guice injector has
     * been built.
     * <p>
     * {@link FormExtensionsRegistry} is a lazily-initialized singleton. Its constructor injects
     * Polarion's own (Guice-bound) form extensions — {@code velocity_form}, {@code oslc},
     * {@code execute-test}, {@code linkedResources}, workflow-signatures widget, {@code gitlab}, …
     * via {@code GuicePlatform.tryInjectMembers}. If the very first {@code getInstance()} call
     * happens during OSGi bundle activation, before {@link GuicePlatform} has built the global
     * injector, {@code tryInjectMembers} silently injects nothing: the singleton is created with an
     * empty contribution set and every Polarion-provided form extension is lost for the whole
     * server lifetime (the visible symptom is e.g. "Form extension 'velocity_form' was not found").
     * <p>
     * Waiting for the injector guarantees that the registry first loads Polarion's core extensions;
     * only then do we add ours on top via {@link FormExtensionsRegistry#registerExtension}.
     */
    void registerExtensionsWhenReady(@NotNull Map<String, IFormExtension> extensions) {
        if (!awaitGuicePlatform()) {
            logger.warn("Polarion's Guice platform did not become ready within "
                    + PLATFORM_WAIT_TIMEOUT_MS + " ms; registering form extensions anyway. "
                    + "Polarion's own form extensions may be unavailable.");
        }
        // Serialize registration across bundles: several activators may cross the readiness barrier
        // at once, and FormExtensionsRegistry's backing map is not synchronized.
        synchronized (GenericBundleActivator.class) {
            extensions.forEach((key, value) -> {
                logger.info("Registering form extension: " + key);
                FormExtensionsRegistry.getInstance().registerExtension(key, value);
            });
        }
    }

    /**
     * Blocks until Polarion's global Guice injector is available or the timeout elapses.
     *
     * @return {@code true} if the injector became available, {@code false} on timeout
     */
    private boolean awaitGuicePlatform() {
        long deadline = System.currentTimeMillis() + PLATFORM_WAIT_TIMEOUT_MS;
        while (!isGuicePlatformReady()) {
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            try {
                Thread.sleep(PLATFORM_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return isGuicePlatformReady();
            }
        }
        return true;
    }

    /**
     * Probes {@code GuicePlatform.getGlobalInjector()} reflectively — it throws while the platform
     * is not yet initialized, which is what we treat as "not ready". Reflection is used only to
     * avoid a compile-time dependency on Guice ({@code com.google.inject.Injector}, the method's
     * return type), which this framework does not otherwise need. Overridable so tests can drive
     * readiness without touching the Guice platform.
     *
     * @return {@code true} once the global injector resolves without throwing
     */
    protected boolean isGuicePlatformReady() {
        if (GET_GLOBAL_INJECTOR == null) {
            return true; // cannot probe -> fail open, don't block registration forever
        }
        try {
            GET_GLOBAL_INJECTOR.invoke(null);
            return true;
        } catch (InvocationTargetException notReadyYet) {
            return false; // getGlobalInjector() threw -> global injector not built yet
        } catch (IllegalAccessException e) {
            logger.error("Unable to probe Guice platform readiness", e);
            return true; // fail open
        }
    }

    @SuppressWarnings({"java:S1181", "java:S2139"}) // catch Throwable: a static-init probe must never fail <clinit>
    private static Method resolveGetGlobalInjector() {
        try {
            return GuicePlatform.class.getMethod("getGlobalInjector");
        } catch (Throwable e) {
            // NoSuchMethodException, or — e.g. in a unit-test JVM without Guice on the classpath — a
            // LinkageError while resolving the method's return type (com.google.inject.Injector).
            // Either way the probe is unavailable and isGuicePlatformReady() fails open.
            logger.error("Cannot resolve GuicePlatform.getGlobalInjector(); platform-readiness probing disabled", e);
            return null;
        }
    }
}
