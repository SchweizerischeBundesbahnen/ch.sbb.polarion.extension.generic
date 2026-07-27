package ch.sbb.polarion.extension.generic;

import com.polarion.alm.ui.server.forms.extensions.FormExtensionContribution;
import com.polarion.alm.ui.server.forms.extensions.IFormExtension;
import com.polarion.alm.ui.server.forms.extensions.impl.FormExtensionsRegistry;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.guice.ipi.GuicePlatform;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Simplifies the way of registering form extension(s).
 * <p>
 * Both the {@link #onStart(BundleContext)} hook and form-extension registration run on a background
 * thread, and only once Polarion's global Guice injector is available — see
 * {@link #startWhenReady(BundleContext)} for why this deferral is required.
 */
@SuppressWarnings("unused")
public abstract class GenericBundleActivator implements BundleActivator {

    private static final Logger logger = Logger.getLogger(GenericBundleActivator.class);

    /** How long to wait for Polarion's global Guice injector before registering anyway. */
    private static final long PLATFORM_WAIT_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(5);
    /** How often to poll for the global Guice injector while waiting. */
    private static final long PLATFORM_POLL_INTERVAL_MS = 200;

    /** The deferred-registration thread, so {@link #stop(BundleContext)} can cancel a pending wait. */
    private final AtomicReference<Thread> registrar = new AtomicReference<>();

    protected abstract Map<String, IFormExtension> getExtensions();

    protected void onStart(BundleContext context) {
        // for overriding if needed
    }

    @Override
    public void start(BundleContext context) {
        // Defer the whole startup — the onStart() hook and form-extension registration — onto a
        // background thread that first waits for Polarion's global Guice injector, see
        // startWhenReady(). start() must stay non-blocking: the global injector may still be built
        // later on this very thread, so blocking here could deadlock startup.
        runAsync(() -> startWhenReady(context));
    }

    @Override
    public void stop(BundleContext context) {
        // Cancel a still-pending deferred startup so a stopped/hot-reloaded bundle does not run
        // onStart() or register stale extensions once (or if) the Guice platform becomes ready.
        Thread current = registrar.get();
        if (current != null) {
            current.interrupt();
        }
    }

    /**
     * Runs the deferred startup task. Spawns a daemon thread by default; overridable so tests can
     * execute it synchronously.
     */
    protected void runAsync(@NotNull Runnable task) {
        Thread thread = new Thread(task, "generic-form-extension-registrar");
        thread.setDaemon(true);
        registrar.set(thread);
        thread.start();
    }

    /**
     * Deferred startup: waits for Polarion's global Guice injector, then runs the subclass
     * {@link #onStart(BundleContext)} hook and registers this bundle's form extensions.
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
     * only then do we run {@link #onStart(BundleContext)} and add ours on top. onStart() is deferred
     * past the barrier too, so that a subclass hook touching {@code FormExtensionsRegistry.getInstance()}
     * cannot poison the registry either. A wait interrupted via {@link #stop(BundleContext)} aborts
     * startup entirely.
     */
    void startWhenReady(@NotNull BundleContext context) {
        try {
            if (!awaitGuicePlatform()) {
                logger.warn("Polarion's Guice platform did not become ready within "
                        + getPlatformWaitTimeoutMs() + " ms; starting anyway. "
                        + "Polarion's own form extensions may be unavailable.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.info("Bundle startup cancelled before the Guice platform was ready (bundle stopping)");
            return;
        }
        onStart(context);
        registerExtensions(getExtensions());
    }

    @SuppressWarnings("java:S5803")
    private void registerExtensions(@NotNull Map<String, IFormExtension> extensions) {
        // Serialize registration across bundles: several activators may cross the readiness barrier
        // at once, and FormExtensionsRegistry's backing map is not synchronized. Lock on a monitor
        // shared by every bundle: FormExtensionsRegistry comes from Polarion's shared bundle, so its
        // Class is the same object in every extension, whereas each bundle loads its own
        // GenericBundleActivator Class (bundle-private classloaders) — locking on the latter would
        // NOT serialize across bundles. Referencing the Class does not construct the singleton.
        synchronized (FormExtensionsRegistry.class) {
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
     * @throws InterruptedException if the wait is interrupted (e.g. by {@link #stop(BundleContext)})
     */
    private boolean awaitGuicePlatform() throws InterruptedException {
        long deadline = System.currentTimeMillis() + getPlatformWaitTimeoutMs();
        while (true) {
            // Honour cancellation up front: if the injector is already ready the loop would
            // otherwise never sleep, so an interrupt set by stop() would go unobserved.
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            if (isGuicePlatformReady()) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            Thread.sleep(getPlatformPollIntervalMs());
        }
    }

    /**
     * Probes the global Guice injector the same way {@link FormExtensionsRegistry} does: while the
     * platform is not yet initialized {@code GuicePlatform.tryInjectMembers} is a no-op, so the
     * probe's {@code @Inject} field stays {@code null}; once the injector exists the (always-bound)
     * {@code Set<FormExtensionContribution>} gets injected. Using {@code tryInjectMembers} keeps
     * this free of any compile-time dependency on Guice. Overridable so tests can drive readiness.
     *
     * @return {@code true} once the global injector injects the probe
     */
    protected boolean isGuicePlatformReady() {
        ReadinessProbe probe = new ReadinessProbe();
        GuicePlatform.tryInjectMembers(probe);
        return probe.contributions != null;
    }

    protected long getPlatformWaitTimeoutMs() {
        return PLATFORM_WAIT_TIMEOUT_MS;
    }

    protected long getPlatformPollIntervalMs() {
        return PLATFORM_POLL_INTERVAL_MS;
    }

    /** Member-injection target used only to detect that the global Guice injector is available. */
    static final class ReadinessProbe {
        // Field (not constructor) injection is required: Guice populates this via tryInjectMembers()
        // on an already-constructed probe, exactly as Polarion's own FormExtensionsRegistry does.
        @SuppressWarnings("java:S6813")
        @Inject
        Set<FormExtensionContribution> contributions;
    }
}
