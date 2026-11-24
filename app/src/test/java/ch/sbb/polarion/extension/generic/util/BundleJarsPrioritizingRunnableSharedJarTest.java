package ch.sbb.polarion.extension.generic.util;

import com.sun.net.httpserver.HttpServer;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

/**
 * In Polarion the bundle jars are not {@code file:} URLs, so {@link java.net.URLClassLoader} takes their
 * {@link java.util.jar.JarFile} from the JVM-wide {@code JarFileFactory} cache, and every class loader over the bundle
 * shares one instance. An HTTP URL takes the same path, which a {@code file:} URL from the test classpath does not.
 */
class BundleJarsPrioritizingRunnableSharedJarTest {

    private static final String FIRST_CLASS = "probe.FirstLazyClass";
    private static final String SECOND_CLASS = "probe.SecondLazyClass";

    private HttpServer server;
    private URL jarUrl;

    @BeforeEach
    void startServer() throws IOException, URISyntaxException {
        byte[] jar = probeJar();
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/bundle.jar", exchange -> {
            exchange.sendResponseHeaders(200, jar.length);
            try (OutputStream body = exchange.getResponseBody()) {
                body.write(jar);
            }
        });
        server.start();
        jarUrl = new URI("http", null, InetAddress.getLoopbackAddress().getHostAddress(), server.getAddress().getPort(), "/bundle.jar", null, null).toURL();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void execute_shouldNotBreakCachedRunnerSharingTheJar() {
        try (MockedStatic<BundleJarsPrioritizingRunnableExecutor> executor = mockStatic(BundleJarsPrioritizingRunnableExecutor.class, CALLS_REAL_METHODS)) {
            executor.when(() -> BundleJarsPrioritizingRunnableExecutor.getBundleJarURLs(any())).thenReturn(new URL[]{jarUrl});

            assertEquals(FIRST_CLASS, load(() -> BundleJarsPrioritizingRunnable.executeCached(ContextClassLoadingRunnable.class, params(FIRST_CLASS), true)));
            assertEquals(FIRST_CLASS, load(() -> BundleJarsPrioritizingRunnable.execute(ContextClassLoadingRunnable.class, params(FIRST_CLASS), true)));

            // the cached runner loads a class it has not loaded yet, from the jar the call above used too
            assertEquals(SECOND_CLASS, load(() -> BundleJarsPrioritizingRunnable.executeCached(ContextClassLoadingRunnable.class, params(SECOND_CLASS), true)));
        }
    }

    private static Map<String, Object> params(String className) {
        return Map.of(ContextClassLoadingRunnable.CLASS_NAME, className);
    }

    private static Object load(java.util.function.Supplier<Map<String, Object>> call) {
        return call.get().get(ContextClassLoadingRunnable.LOADED);
    }

    private static byte[] probeJar() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JarOutputStream jar = new JarOutputStream(bytes)) {
            for (String className : new String[]{FIRST_CLASS, SECOND_CLASS}) {
                jar.putNextEntry(new JarEntry(className.replace('.', '/') + ".class"));
                try (DynamicType.Unloaded<Object> type = new ByteBuddy().subclass(Object.class).name(className).make()) {
                    jar.write(type.getBytes());
                }
                jar.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    /**
     * Its own class comes from the test classpath, so it loads the probe classes through the bundle class loader
     * the executor puts on the thread.
     */
    public static class ContextClassLoadingRunnable implements BundleJarsPrioritizingRunnable {

        static final String CLASS_NAME = "className";
        static final String LOADED = "loaded";

        @Override
        public Map<String, Object> run(Map<String, Object> params) {
            try {
                Class<?> loaded = Class.forName((String) params.get(CLASS_NAME), true, Thread.currentThread().getContextClassLoader());
                return Map.of(LOADED, loaded.getName());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
