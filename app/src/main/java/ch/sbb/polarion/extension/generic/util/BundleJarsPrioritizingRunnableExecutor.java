package ch.sbb.polarion.extension.generic.util;

import com.polarion.core.util.logging.Logger;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Constants;

import java.io.NotSerializableException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Implementation class for {@link BundleJarsPrioritizingRunnable} execution logic.
 * This class is package-private and should not be used directly.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings({"unchecked", "java:S1905", "java:S1141"})
final class BundleJarsPrioritizingRunnableExecutor {

    private static final String PLUGIN_XML = "plugin.xml";
    private static final Map<Class<?>, CachedRunner> RUNNER_CACHE = new ConcurrentHashMap<>();

    record CachedRunner(BundleJarsPrioritizingRunnable.BundleJarsPrioritizingClassLoader classLoader, Object instance, Class<?> runnableClass) {}

    @SneakyThrows
    static @NotNull Map<String, Object> executeCached(@NotNull Class<? extends BundleJarsPrioritizingRunnable> runnableImplClass, @NotNull Map<String, Object> params, boolean rethrowException) {
        ClassLoader originalThreadClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            CachedRunner cached = RUNNER_CACHE.computeIfAbsent(runnableImplClass, clazz -> createRunner(runnableImplClass, originalThreadClassLoader));

            Thread.currentThread().setContextClassLoader(cached.classLoader());

            return invokeRunnable(cached.runnableClass(), cached.instance(), params);
        } catch (Exception e) {
            Logger.getLogger(BundleJarsPrioritizingRunnable.class).error("Error while running cached impl", e);
            if (rethrowException) {
                throw e;
            } else {
                return Map.of(BundleJarsPrioritizingRunnable.ERROR_KEY, e);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalThreadClassLoader);
        }
    }

    @SneakyThrows
    private static CachedRunner createRunner(@NotNull Class<? extends BundleJarsPrioritizingRunnable> runnableImplClass, ClassLoader originalThreadClassLoader) {
        List<URL> bundleJarURLs = collectBundleURLs(runnableImplClass);

        BundleJarsPrioritizingRunnable.BundleJarsPrioritizingClassLoader classLoader = new BundleJarsPrioritizingRunnable.BundleJarsPrioritizingClassLoader(
                bundleJarURLs.toArray(new URL[0]),
                runnableImplClass.getClassLoader(),
                originalThreadClassLoader);

        try {
            Class<?> runnableClass = classLoader.loadClass(runnableImplClass.getName());
            Object instance = runnableClass.getDeclaredConstructor().newInstance();

            return new CachedRunner(classLoader, instance, runnableClass);
        } catch (Exception e) {
            classLoader.close();
            throw e;
        }
    }

    @SneakyThrows
    static @NotNull Map<String, Object> execute(@NotNull Class<? extends BundleJarsPrioritizingRunnable> runnableImplClass, @NotNull Map<String, Object> params, boolean rethrowException) {
        ClassLoader originalThreadClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            List<URL> bundleJarURLs = collectBundleURLs(runnableImplClass);

            try (BundleJarsPrioritizingRunnable.BundleJarsPrioritizingClassLoader classLoader = new BundleJarsPrioritizingRunnable.BundleJarsPrioritizingClassLoader(bundleJarURLs.toArray(new URL[0]), runnableImplClass.getClassLoader(), originalThreadClassLoader)) {

                Thread.currentThread().setContextClassLoader(classLoader);

                Class<?> runnableClass = classLoader.loadClass(runnableImplClass.getName());
                Object runnable = runnableClass.getDeclaredConstructor().newInstance();

                return invokeRunnable(runnableClass, runnable, params);
            }
        } catch (Exception e) {
            Logger.getLogger(BundleJarsPrioritizingRunnable.class).error("Error while running impl", e);
            if (rethrowException) {
                throw e;
            } else {
                return Map.of(BundleJarsPrioritizingRunnable.ERROR_KEY, e);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalThreadClassLoader);
        }
    }

    static URL[] getBundleJarURLs(Class<?> bundleClass) {
        return Stream.of(ManifestUtils.getManifestAttributes().getValue(Constants.BUNDLE_CLASSPATH).split(","))
                .filter(p -> !p.equals("."))
                .map(p -> bundleClass.getResource("/" + p))
                .toArray(URL[]::new);
    }

    @SneakyThrows
    private static List<URL> collectBundleURLs(Class<?> bundleClass) {
        List<URL> bundleJarURLs = new ArrayList<>(Arrays.asList(getBundleJarURLs(bundleClass)));

        // add the bundle URL itself to handle classes/resources located directly in the bundle
        URL pluginXmlUrl = bundleClass.getResource("/" + PLUGIN_XML);
        if (pluginXmlUrl != null) {
            bundleJarURLs.add(URI.create(pluginXmlUrl.toString().replace(PLUGIN_XML, "")).toURL());
        }

        return bundleJarURLs;
    }

    /**
     * Invokes the runnable with serialized or raw params.
     * We attempt to use serialization for the params where it's possible because now classes inside will be loaded via
     * the new class loader so sometimes this can lead to ClassCastExceptions.
     */
    @SneakyThrows
    private static Map<String, Object> invokeRunnable(Class<?> runnableClass, Object instance, Map<String, Object> params) {
        byte[] paramsSerialized = null;
        try {
            paramsSerialized = ObjectUtils.serialize(params);
        } catch (NotSerializableException e) {
            Logger.getLogger(BundleJarsPrioritizingRunnable.class).debug("Parameters are not serializable; passing them as-is...");
        }

        if (paramsSerialized == null) {
            return (Map<String, Object>) runnableClass.getMethod("run", Map.class).invoke(instance, params);
        } else {
            return (Map<String, Object>) runnableClass.getMethod("runInternal", byte[].class).invoke(instance, (Object) paramsSerialized);
        }
    }
}
