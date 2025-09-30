package ch.sbb.polarion.extension.generic.util;

import com.polarion.core.util.StringUtils;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.guice.internal.GuiceActivator;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import java.io.NotSerializableException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A runnable that ensures its implementation runs with a class loader that prioritizes the bundle's own JARs.
 * This is useful in OSGi environments where class loading can be complex due to multiple bundles.
 * <p>
 * To use, implement this interface and call the static {@link #execute(Class, Map)} method with the implementation class and parameters.
 * The implementation class must have a no-argument constructor.
 * <p>
 * The {@link #run(Map)} method will be called with the provided parameters, and it should return a map of results.
 * <p>
 * Example usage:
 * <pre>
 * Map<String, Object> result = BundleJarsPrioritizingRunnable.execute(MyRunnableImpl.class, params);
 * </pre>
 */
@SuppressWarnings({"unused", "unchecked", "java:S1905"})
public interface BundleJarsPrioritizingRunnable {

    String ERROR_KEY = "BundleJarsPrioritizingRunnableError";

    @SneakyThrows
    static @NotNull Map<String, Object> execute(@NotNull Class<? extends BundleJarsPrioritizingRunnable> runnableImplClass, @NotNull Map<String, Object> params, boolean rethrowException) {
        ClassLoader originalThreadClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            List<URL> bundleJarURLs = new ArrayList<>(Arrays.asList(getBundleJarURLs(runnableImplClass)));

            // add the bundle URL itself to handle classes/resources located directly in the bundle
            URL pluginXmlUrl = runnableImplClass.getResource("/plugin.xml");
            if (pluginXmlUrl != null) {
                bundleJarURLs.add(new URL(pluginXmlUrl.toString().replace("plugin.xml", "")));
            }

            try (BundleJarsPrioritizingClassLoader classLoader = new BundleJarsPrioritizingClassLoader(bundleJarURLs.toArray(new URL[0]), runnableImplClass.getClassLoader(), originalThreadClassLoader)) {

                Thread.currentThread().setContextClassLoader(classLoader);

                // we attempt to use serialization for the params where it's possible because now classes inside will be loaded via
                // the new class loader so sometimes this can lead to ClassCastExceptions
                byte[] paramsSerialized = null;
                try {
                    paramsSerialized = ObjectUtils.serialize(params);
                } catch (NotSerializableException e) {
                    Logger.getLogger(BundleJarsPrioritizingRunnable.class).debug("Parameters are not serializable; passing them as-is...");
                }

                Class<?> runnableClass = classLoader.loadClass(runnableImplClass.getName());
                Object runnable = runnableClass.getDeclaredConstructor().newInstance();
                if (paramsSerialized == null) {
                    return (Map<String, Object>) runnableClass.getMethod("run", Map.class).invoke(runnable, params);
                } else {
                    return (Map<String, Object>) runnableClass.getMethod("runInternal", byte[].class).invoke(runnable, (Object) paramsSerialized);
                }
            }
        } catch (Exception e) {
            Logger.getLogger(BundleJarsPrioritizingRunnable.class).error("Error while running impl", e);
            if (rethrowException) {
                throw e;
            } else {
                return Map.of(ERROR_KEY, e);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalThreadClassLoader);
        }
    }

    static @NotNull Map<String, Object> execute(@NotNull Class<? extends BundleJarsPrioritizingRunnable> runnableImplClass, @NotNull Map<String, Object> params) {
        return execute(runnableImplClass, params, false);
    }

    static URL[] getBundleJarURLs(Class<?> bundleClass) {
        return Stream.of(ManifestUtils.getManifestAttributes().getValue(Constants.BUNDLE_CLASSPATH).split(","))
                .filter(p -> !p.equals("."))
                .map(p -> bundleClass.getResource("/" + p))
                .toArray(URL[]::new);
    }

    @SneakyThrows
    default Map<String, Object> runInternal(byte[] paramsSerialized) {
        return run((Map<String, Object>) ObjectUtils.deserialize(paramsSerialized));
    }

    /**
     * The main method to be implemented by the user.
     * This method will be called with the provided parameters and should return a map of results.
     *
     * @param params a map of parameters to be used in the implementation
     * @return a map of results from the implementation
     */
    Map<String, Object> run(Map<String, Object> params);

    /**
     * A custom class loader that prioritizes loading classes from the bundle's own JARs.
     * If a class is not found in the bundle's JARs, it attempts to load it from the provided second priority class loaders.
     * If still not found, it tries to load the class from other bundles' exported packages.
     */
    class BundleJarsPrioritizingClassLoader extends URLClassLoader {

        private final ClassLoader[] secondPriorityClassLoaders;

        public BundleJarsPrioritizingClassLoader(URL[] bundleJarURLs, ClassLoader... secondPriorityClassLoaders) {
            super(bundleJarURLs);
            this.secondPriorityClassLoaders = secondPriorityClassLoaders;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            try {
                // First try to load the class from the current's bundle jars
                return super.findClass(name);
            } catch (ClassNotFoundException e) {

                // If not found, try the second priority class loaders
                for (ClassLoader classLoader : secondPriorityClassLoaders) {
                    try {
                        return classLoader.loadClass(name);
                    } catch (ClassNotFoundException var8) {
                        // Ignore and try next
                    }
                }

                // Finally, try to load the class from other bundles' exported packages
                for (Bundle bundle : GuiceActivator.bundleContext.getBundles()) {
                    try {
                        if (name.contains(".") && StringUtils.getEmptyIfNull(bundle.getHeaders().get(Constants.EXPORT_PACKAGE)).contains(name.substring(0, name.lastIndexOf('.')))) {
                            return bundle.loadClass(name);
                        }
                    } catch (ClassNotFoundException var8) {
                        // Ignore and try next
                    }
                }

                throw new ClassNotFoundException(name);
            }
        }
    }
}
