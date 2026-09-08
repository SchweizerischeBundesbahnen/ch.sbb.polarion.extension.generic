package ch.sbb.polarion.extension.generic.util;

import com.polarion.alm.administration.web.server.AdministrationPageExtender;
import com.polarion.alm.administration.web.server.AdministrationPageExtenderProvider;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.core.IPlatform;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.guice.ipi.GuicePlatform;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Restores the declaration order of the administration menu entries contributed through
 * {@code com.polarion.xray.webui.administrationPageExtenders}.
 * <p>
 * Up to Polarion 2512 {@code AdministrationPageExtenderProvider} received the HiveMind configuration
 * as a {@code List}, so the menu followed the order of {@code hivemodule.xml}. Polarion 2606 routes the
 * same configuration through {@code Set.copyOf} in
 * {@code AbstractHiveMindPlatform.lookupSetConfigurationPoint} and injects it as a {@code Set}. That is
 * a JDK immutable set whose iteration order follows element hash codes salted once per JVM start, and
 * {@code AdministrationPageExtender} does not override {@code hashCode}. Every menu entry of every
 * extension therefore lands in a different place after each restart. The extension point has no order
 * attribute and nothing sorts the entries afterward.
 * <p>
 * The HiveMind registry still holds the configuration in declaration order, and the provider keeps its
 * entries in a mutable list. This class copies the registry order into that list, which restores the
 * behavior of Polarion 2512 exactly: the same objects in the same order the platform used to pass in.
 * <p>
 * The registry is reached through two public methods ({@code IHiveMindPlatform.getRegistry()} and
 * {@code Registry.getConfiguration(String)}), reflectively so that the {@code org.apache.hivemind} types
 * stay off the compile classpath and out of {@code Require-Bundle}. The provider's list is read from a
 * private field, which has no public equivalent. Anything unexpected is logged and leaves Polarion's own
 * order in place.
 * <p>
 * The provider must come from Guice, not from {@code PlatformContext.lookupService}. Its HiveMind
 * service point uses the default singleton service model, whose {@code getService()} returns a generated
 * proxy: the proxy carries no field to read, so the reflective read finds nothing. Member injection
 * yields the real singleton the proxy delegates to, which is also the object that builds the navigation
 * tree.
 */
public final class AdministrationMenuOrderRestorer {

    static final String CONFIG_ID = "com.polarion.xray.webui.administrationPageExtenders";
    private static final String EXTENDERS_FIELD = "extenders";
    private static final String GET_REGISTRY_METHOD = "getRegistry";
    private static final String GET_CONFIGURATION_METHOD = "getConfiguration";

    private static final Logger logger = Logger.getLogger(AdministrationMenuOrderRestorer.class);

    private AdministrationMenuOrderRestorer() {
    }

    /**
     * Reorders the administration menu entries of every extension into their declaration order.
     * <p>
     * Requires Polarion's platform to be initialized, so call it only after the global Guice injector is
     * available. Safe to call repeatedly and from several bundles: each call computes the same order, and
     * the work is serialized on the shared list.
     */
    public static void restoreDeclarationOrder() {
        try {
            AdministrationPageExtenderProvider provider = lookupProvider();
            if (provider == null) {
                logger.warn("Administration menu order was not restored: the page extender provider was not injected");
                return;
            }
            restoreDeclarationOrder(provider, PlatformContext.getPlatform());
        } catch (Exception | LinkageError e) {
            // LinkageError included: every Polarion type named here can move in a future release, and a
            // cosmetic menu fix must never take the calling bundle's startup down with it.
            logger.warn("Administration menu order was not restored, Polarion's own order is kept", e);
        }
    }

    /**
     * The reordering itself, with the two things it needs handed in so it can be driven without a running
     * Polarion.
     *
     * @param provider the provider holding every extension's menu entries
     * @param platform the platform to read the HiveMind configuration from
     */
    static void restoreDeclarationOrder(@NotNull AdministrationPageExtenderProvider provider, @NotNull IPlatform platform) throws ReflectiveOperationException {
        List<Object> liveOrder = readLiveOrder(provider);
        if (liveOrder == null) {
            logger.warn("Administration menu order was not restored: %s has no readable '%s' list"
                    .formatted(provider.getClass().getName(), EXTENDERS_FIELD));
            return;
        }
        List<?> declaredOrder = readDeclaredOrder(platform);
        if (declaredOrder == null) {
            logger.warn("Administration menu order was not restored: the '%s' configuration is not readable".formatted(CONFIG_ID));
            return;
        }
        // The list is shared by every extension bundle, and each one runs this on its own thread. Locking
        // on the list itself serializes them against each other, unlike locking on a class of this
        // bundle: every extension loads its own copy of these classes. The lock also covers the
        // already-ordered check, so exactly one bundle does the work and every later one finds it done.
        // It orders nothing against Polarion's readers, which never take this monitor, see
        // restore(List, List) for what a concurrent reader can see.
        Outcome outcome;
        synchronized (liveOrder) {
            outcome = restore(liveOrder, declaredOrder);
        }
        switch (outcome) {
            case RESTORED -> {
                logger.info("Restored the declaration order of %d administration menu entries".formatted(declaredOrder.size()));
                logger.debug(() -> "Administration menu order: %s".formatted(describe(declaredOrder)));
            }
            case ALREADY_ORDERED -> logger.debug(() -> "Administration menu already holds its %d entries in declaration order, another bundle restored it"
                    .formatted(declaredOrder.size()));
            case MISMATCH -> logger.warn("Administration menu order was not restored: Polarion's menu entries do not match its own configuration");
        }
    }

    /**
     * Copies {@code declaredOrder} into {@code liveOrder} position by position.
     * <p>
     * Only a permutation is applied, and only when both lists hold exactly the same objects. The size
     * never changes and no position is ever empty, so a reader building the navigation tree in parallel
     * cannot observe a null entry. It can, while the loop runs, briefly see one entry twice and another
     * not at all, and holding no lock this class holds, it is not guaranteed to observe the new order at
     * all. Both are acceptable here: the window is a few microseconds during bundle activation, before
     * administration pages are served. Rejecting anything but a permutation keeps a future Polarion,
     * which may fill the provider from somewhere else, from having its entries replaced.
     *
     * @param liveOrder     the provider's own list, modified in place
     * @param declaredOrder the same entries in declaration order
     * @return what the call did, see {@link Outcome}
     */
    static @NotNull Outcome restore(@NotNull List<Object> liveOrder, @NotNull List<?> declaredOrder) {
        if (liveOrder.size() != declaredOrder.size() || !holdSameEntries(liveOrder, declaredOrder)) {
            return Outcome.MISMATCH;
        }
        if (alreadyOrdered(liveOrder, declaredOrder)) {
            return Outcome.ALREADY_ORDERED;
        }
        for (int i = 0; i < declaredOrder.size(); i++) {
            liveOrder.set(i, declaredOrder.get(i));
        }
        return Outcome.RESTORED;
    }

    /** What a {@link #restore(List, List)} call did. */
    enum Outcome {
        /** The order was applied. */
        RESTORED,
        /** Nothing to do: the entries already sat in declaration order. */
        ALREADY_ORDERED,
        /** The two lists do not hold the same entries, so nothing was touched. */
        MISMATCH
    }

    private static boolean alreadyOrdered(@NotNull List<?> liveOrder, @NotNull List<?> declaredOrder) {
        for (int i = 0; i < declaredOrder.size(); i++) {
            if (liveOrder.get(i) != declaredOrder.get(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Renders the applied order for the log, so the result is verifiable on a running server without a
     * debugger. Entries are named {@code parentNodeId/id}.
     */
    static @NotNull String describe(@NotNull List<?> entries) {
        return entries.stream()
                .map(entry -> entry instanceof AdministrationPageExtender extender
                        ? "%s/%s".formatted(extender.getParentNodeId(), extender.getId())
                        : String.valueOf(entry))
                .collect(Collectors.joining(", "));
    }

    /**
     * Compares by identity rather than by {@code equals}: {@code AdministrationPageExtender} does not
     * implement {@code equals}, and a repeated instance must count as a mismatch.
     */
    private static boolean holdSameEntries(@NotNull List<?> first, @NotNull List<?> second) {
        Set<Object> firstEntries = identitySet(first);
        // The size check also rejects a repeated instance: a set collapses repeats, so equal sets over
        // equal-sized lists would otherwise admit one entry being dropped and another written twice.
        return firstEntries.size() == first.size() && firstEntries.equals(identitySet(second));
    }

    private static @NotNull Set<Object> identitySet(@NotNull List<?> entries) {
        Set<Object> result = Collections.newSetFromMap(new IdentityHashMap<>());
        result.addAll(entries);
        return result;
    }

    /**
     * Obtains the real provider singleton by member injection, the same way
     * {@code GenericBundleActivator} probes the platform readiness. {@code UIModule} binds the concrete
     * class as a Guice singleton, so this is the instance Polarion's own navigation code reaches through
     * its HiveMind proxy.
     */
    static @Nullable AdministrationPageExtenderProvider lookupProvider() {
        ProviderProbe probe = new ProviderProbe();
        GuicePlatform.tryInjectMembers(probe);
        return probe.provider;
    }

    /**
     * Reads the provider's own list of entries. Walks the class hierarchy so a future subclass of the
     * provider is still handled.
     */
    @SuppressWarnings({"unchecked", "java:S3011"})
    static @Nullable List<Object> readLiveOrder(@NotNull AdministrationPageExtenderProvider provider) throws ReflectiveOperationException {
        for (Class<?> type = provider.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(EXTENDERS_FIELD);
                field.setAccessible(true);
                Object value = field.get(provider);
                return value instanceof List ? (List<Object>) value : null;
            } catch (NoSuchFieldException e) {
                // Keep looking further up the hierarchy.
            }
        }
        return null;
    }

    /**
     * Reads the configuration straight from the HiveMind registry, the only place that still holds the
     * entries in declaration order.
     */
    @SuppressWarnings("unchecked")
    static @Nullable List<Object> readDeclaredOrder(@NotNull IPlatform platform) throws ReflectiveOperationException {
        Method getRegistry = findMethod(platform.getClass(), GET_REGISTRY_METHOD);
        if (getRegistry == null) {
            return null;
        }
        Object registry = getRegistry.invoke(platform);
        if (registry == null) {
            return null;
        }
        Method getConfiguration = findMethod(registry.getClass(), GET_CONFIGURATION_METHOD, String.class);
        if (getConfiguration == null) {
            return null;
        }
        Object configuration = getConfiguration.invoke(registry, CONFIG_ID);
        // The elements are only ever read and handed to restore(), so the cast carries no risk. Typed
        // rather than left as a wildcard to match readLiveOrder and to keep the return type usable.
        return configuration instanceof List<?> list && !list.isEmpty() ? (List<Object>) list : null;
    }

    static @Nullable Method findMethod(@NotNull Class<?> type, @NotNull String name, @NotNull Class<?>... parameterTypes) {
        try {
            return type.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            logger.warn("Administration menu order was not restored: %s has no %s method".formatted(type.getName(), name));
            return null;
        }
    }

    /** Member-injection target used only to obtain the provider singleton, see {@link #lookupProvider()}. */
    static final class ProviderProbe {
        // Field injection is required: Guice populates this via tryInjectMembers() on an
        // already-constructed probe, which keeps this class free of Guice types.
        @SuppressWarnings("java:S6813")
        @Inject
        AdministrationPageExtenderProvider provider;
    }
}
