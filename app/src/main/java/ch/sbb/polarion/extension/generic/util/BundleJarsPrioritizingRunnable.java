package ch.sbb.polarion.extension.generic.util;

import com.polarion.core.util.StringUtils;
import com.polarion.platform.guice.internal.GuiceActivator;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

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
 * Map&lt;String, Object&gt; result = BundleJarsPrioritizingRunnable.execute(MyRunnableImpl.class, params);
 * </pre>
 */
@SuppressWarnings("unused")
public interface BundleJarsPrioritizingRunnable {

    String ERROR_KEY = "BundleJarsPrioritizingRunnableError";

    static @NotNull Map<String, Object> executeCached(@NotNull Class<? extends BundleJarsPrioritizingRunnable> runnableImplClass, @NotNull Map<String, Object> params, boolean rethrowException) {
        return BundleJarsPrioritizingRunnableExecutor.executeCached(runnableImplClass, params, rethrowException);
    }

    static @NotNull Map<String, Object> executeCached(@NotNull Class<? extends BundleJarsPrioritizingRunnable> runnableImplClass, @NotNull Map<String, Object> params) {
        return BundleJarsPrioritizingRunnableExecutor.executeCached(runnableImplClass, params, false);
    }

    static @NotNull Map<String, Object> execute(@NotNull Class<? extends BundleJarsPrioritizingRunnable> runnableImplClass, @NotNull Map<String, Object> params, boolean rethrowException) {
        return BundleJarsPrioritizingRunnableExecutor.execute(runnableImplClass, params, rethrowException);
    }

    static @NotNull Map<String, Object> execute(@NotNull Class<? extends BundleJarsPrioritizingRunnable> runnableImplClass, @NotNull Map<String, Object> params) {
        return BundleJarsPrioritizingRunnableExecutor.execute(runnableImplClass, params, false);
    }

    static URL[] getBundleJarURLs(Class<?> bundleClass) {
        return BundleJarsPrioritizingRunnableExecutor.getBundleJarURLs(bundleClass);
    }

    /**
     * Internal method used to deserialize parameters and delegate to {@link #run(Map)}.
     * <p>
     * This method is called when parameters are serializable, allowing them to be passed
     * across class loader boundaries without causing {@link ClassCastException}.
     * The serialization/deserialization ensures that classes are loaded by the correct class loader.
     * <p>
     * Typically, this method should not be overridden unless custom deserialization logic is needed.
     *
     * @param paramsSerialized the serialized parameters as a byte array
     * @return a map of results from the implementation
     */
    @SneakyThrows
    @SuppressWarnings("unchecked")
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
                    } catch (ClassNotFoundException ignored) {
                        // Ignore and try next
                    }
                }

                // Finally, try to load the class from other bundles' exported packages
                for (Bundle bundle : GuiceActivator.bundleContext.getBundles()) {
                    try {
                        if (name.contains(".") && StringUtils.getEmptyIfNull(bundle.getHeaders().get(Constants.EXPORT_PACKAGE)).contains(name.substring(0, name.lastIndexOf('.')))) {
                            return bundle.loadClass(name);
                        }
                    } catch (ClassNotFoundException ignored) {
                        // Ignore and try next
                    }
                }

                throw new ClassNotFoundException(name);
            }
        }
    }
}
