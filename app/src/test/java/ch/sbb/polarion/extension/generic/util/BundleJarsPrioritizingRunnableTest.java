package ch.sbb.polarion.extension.generic.util;

import com.polarion.platform.guice.internal.GuiceActivator;
import lombok.Getter;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import java.io.NotSerializableException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unused")
class BundleJarsPrioritizingRunnableTest {

    @BeforeEach
    void setUp() {
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
    }

    @Test
    void execute_withException_shouldReturnErrorMap() {
        // Arrange
        Map<String, Object> params = Map.of("key", "value");

        try (MockedStatic<ManifestUtils> manifestUtils = mockStatic(ManifestUtils.class);
             MockedStatic<ObjectUtils> objectUtils = mockStatic(ObjectUtils.class)) {

            RuntimeException expectedException = new RuntimeException("Test exception");
            manifestUtils.when(ManifestUtils::getManifestAttributes).thenThrow(expectedException);

            // Act
            Map<String, Object> result = BundleJarsPrioritizingRunnable.execute(TestRunnableImpl.class, params);

            // Assert
            assertNotNull(result);
            assertTrue(result.containsKey(BundleJarsPrioritizingRunnable.ERROR_KEY));
            assertEquals(expectedException, result.get(BundleJarsPrioritizingRunnable.ERROR_KEY));
        }
    }

    @Test
    void execute_withExceptionAndRethrowTrue_shouldThrowException() {
        // Arrange
        Map<String, Object> params = Map.of("key", "value");

        try (MockedStatic<ManifestUtils> manifestUtils = mockStatic(ManifestUtils.class)) {
            RuntimeException expectedException = new RuntimeException("Test exception");
            manifestUtils.when(ManifestUtils::getManifestAttributes).thenThrow(expectedException);

            // Act & Assert
            RuntimeException thrownException = assertThrows(RuntimeException.class,
                () -> BundleJarsPrioritizingRunnable.execute(TestRunnableImpl.class, params, true));

            assertEquals("Test exception", thrownException.getMessage());
        }
    }

    @Test
    void execute_withManifestException_shouldHandleException() {
        // Arrange
        Map<String, Object> params = Map.of("key", "value");

        try (MockedStatic<ManifestUtils> manifestUtils = mockStatic(ManifestUtils.class)) {

            RuntimeException manifestException = new RuntimeException("Manifest read failed");
            manifestUtils.when(ManifestUtils::getManifestAttributes).thenThrow(manifestException);

            // Act
            Map<String, Object> result = BundleJarsPrioritizingRunnable.execute(TestRunnableImpl.class, params);

            // Assert
            assertNotNull(result);
            assertTrue(result.containsKey(BundleJarsPrioritizingRunnable.ERROR_KEY));
            assertEquals(manifestException, result.get(BundleJarsPrioritizingRunnable.ERROR_KEY));
        }
    }

    @Test
    void execute_resetsContextClassLoader_afterException() {
        // Arrange
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Map<String, Object> params = Map.of("key", "value");

        try (MockedStatic<ManifestUtils> manifestUtils = mockStatic(ManifestUtils.class)) {
            RuntimeException expectedException = new RuntimeException("Test exception");
            manifestUtils.when(ManifestUtils::getManifestAttributes).thenThrow(expectedException);

            // Act
            BundleJarsPrioritizingRunnable.execute(TestRunnableImpl.class, params);

            // Assert
            assertEquals(originalClassLoader, Thread.currentThread().getContextClassLoader());
        }
    }

    @Test
    void getBundleJarURLs_withValidManifest_shouldReturnURLs() {
        // Arrange
        try (MockedStatic<ManifestUtils> manifestUtils = mockStatic(ManifestUtils.class)) {
            Attributes mockAttributes = mock(Attributes.class);
            manifestUtils.when(ManifestUtils::getManifestAttributes).thenReturn(mockAttributes);
            when(mockAttributes.getValue(Constants.BUNDLE_CLASSPATH)).thenReturn(".,lib/jar1.jar,lib/jar2.jar");

            // Act
            URL[] result = BundleJarsPrioritizingRunnable.getBundleJarURLs(TestRunnableImpl.class);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.length);
        }
    }

    @Test
    void getBundleJarURLs_withOnlyCurrentDirectory_shouldReturnEmptyArray() {
        // Arrange
        try (MockedStatic<ManifestUtils> manifestUtils = mockStatic(ManifestUtils.class)) {
            Attributes mockAttributes = mock(Attributes.class);
            manifestUtils.when(ManifestUtils::getManifestAttributes).thenReturn(mockAttributes);
            when(mockAttributes.getValue(Constants.BUNDLE_CLASSPATH)).thenReturn(".");

            // Act
            URL[] result = BundleJarsPrioritizingRunnable.getBundleJarURLs(TestRunnableImpl.class);

            // Assert
            assertNotNull(result);
            assertEquals(0, result.length);
        }
    }

    @Test
    void getBundleJarURLs_withMixedPaths_shouldFilterCurrentDirectory() {
        // Arrange
        try (MockedStatic<ManifestUtils> manifestUtils = mockStatic(ManifestUtils.class)) {
            Attributes mockAttributes = mock(Attributes.class);
            manifestUtils.when(ManifestUtils::getManifestAttributes).thenReturn(mockAttributes);
            when(mockAttributes.getValue(Constants.BUNDLE_CLASSPATH)).thenReturn(".,lib/jar1.jar,.,lib/jar2.jar,.");

            // Act
            URL[] result = BundleJarsPrioritizingRunnable.getBundleJarURLs(TestRunnableImpl.class);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.length);
        }
    }


    @Test
    void bundleJarsPrioritizingClassLoader_constructor_shouldInitialize() {
        // Arrange
        URL[] urls = new URL[0];
        ClassLoader parent = getClass().getClassLoader();
        ClassLoader secondary = mock(ClassLoader.class);

        // Act
        BundleJarsPrioritizingRunnable.BundleJarsPrioritizingClassLoader classLoader =
            new BundleJarsPrioritizingRunnable.BundleJarsPrioritizingClassLoader(urls, parent, secondary);

        // Assert
        assertNotNull(classLoader);
    }

    @Test
    @SneakyThrows
    void bundleJarsPrioritizingClassLoader_findClass_shouldLoadFromBundleJars() {
        // Arrange - Create a JAR with a test class to load from bundle's own JARs
        // We'll use the test class itself which is available on the classpath
        URL testClassUrl = BundleJarsPrioritizingRunnableTest.class.getProtectionDomain().getCodeSource().getLocation();
        URL[] urls = new URL[]{testClassUrl};
        ClassLoader parent = mock(ClassLoader.class);
        ClassLoader secondary = mock(ClassLoader.class);

        try (BundleJarsPrioritizingRunnable.BundleJarsPrioritizingClassLoader classLoader =
                     new BundleJarsPrioritizingRunnable.BundleJarsPrioritizingClassLoader(urls, parent, secondary)) {

            // Act - Load a class that exists in the bundle's JARs (test classes location)
            Class<?> result = classLoader.findClass(BundleJarsPrioritizingRunnableTest.class.getName());

            // Assert
            assertNotNull(result);
            assertEquals(BundleJarsPrioritizingRunnableTest.class.getName(), result.getName());
            // Secondary class loaders should not be consulted since the class was found in bundle JARs
            verifyNoInteractions(secondary);
        }
    }

    @Test
    void bundleJarsPrioritizingClassLoader_findClass_shouldFallbackToSecondaryClassLoaders() throws ClassNotFoundException {
        // Arrange
        URL[] urls = new URL[0];
        URLClassLoader parent = new URLClassLoader(urls);
        ClassLoader secondary1 = mock(ClassLoader.class);
        ClassLoader secondary2 = mock(ClassLoader.class);

        doThrow(new ClassNotFoundException()).when(secondary1).loadClass("com.test.NonExistentClass");
        doReturn(String.class).when(secondary2).loadClass("com.test.NonExistentClass");

        BundleJarsPrioritizingRunnable.BundleJarsPrioritizingClassLoader classLoader =
            new BundleJarsPrioritizingRunnable.BundleJarsPrioritizingClassLoader(urls, parent, secondary1, secondary2);

        // Act
        Class<?> result = classLoader.findClass("com.test.NonExistentClass");

        // Assert
        assertEquals(String.class, result);
        verify(secondary1).loadClass("com.test.NonExistentClass");
        verify(secondary2).loadClass("com.test.NonExistentClass");
    }

    @Test
    void bundleJarsPrioritizingClassLoader_findClass_shouldFallbackToBundles() throws ClassNotFoundException {
        // Arrange
        URL[] urls = new URL[0];
        URLClassLoader parent = new URLClassLoader(urls);
        ClassLoader secondary = mock(ClassLoader.class);

        Bundle bundle1 = mock(Bundle.class);
        Bundle bundle2 = mock(Bundle.class);
        Bundle[] bundles = {bundle1, bundle2};

        Dictionary<String, String> headers1 = new Hashtable<>();
        headers1.put(Constants.EXPORT_PACKAGE, "com.other.package");

        Dictionary<String, String> headers2 = new Hashtable<>();
        headers2.put(Constants.EXPORT_PACKAGE, "com.test");

        when(bundle1.getHeaders()).thenReturn(headers1);
        when(bundle2.getHeaders()).thenReturn(headers2);

        doThrow(new ClassNotFoundException()).when(secondary).loadClass("com.test.TestClass");
        doReturn(String.class).when(bundle2).loadClass("com.test.TestClass");

        BundleContext bundleContext = mock(BundleContext.class);
        when(bundleContext.getBundles()).thenReturn(bundles);

        try (MockedStatic<GuiceActivator> guiceActivator = mockStatic(GuiceActivator.class)) {
            GuiceActivator.bundleContext = bundleContext;

            BundleJarsPrioritizingRunnable.BundleJarsPrioritizingClassLoader classLoader =
                new BundleJarsPrioritizingRunnable.BundleJarsPrioritizingClassLoader(urls, parent, secondary);

            // Act
            Class<?> result = classLoader.findClass("com.test.TestClass");

            // Assert
            assertEquals(String.class, result);
            verify(secondary).loadClass("com.test.TestClass");
            verify(bundle2).loadClass("com.test.TestClass");
        }
    }

    @Test
    void bundleJarsPrioritizingClassLoader_findClass_shouldThrowClassNotFoundException() throws ClassNotFoundException {
        // Arrange
        URL[] urls = new URL[0];
        URLClassLoader parent = new URLClassLoader(urls);
        ClassLoader secondary = mock(ClassLoader.class);

        Bundle bundle = mock(Bundle.class);
        Bundle[] bundles = {bundle};

        Dictionary<String, String> headers = new Hashtable<>();
        headers.put(Constants.EXPORT_PACKAGE, "com.other.package");

        when(bundle.getHeaders()).thenReturn(headers);
        doThrow(new ClassNotFoundException()).when(secondary).loadClass("com.test.NonExistentClass");

        BundleContext bundleContext = mock(BundleContext.class);
        when(bundleContext.getBundles()).thenReturn(bundles);

        try (MockedStatic<GuiceActivator> guiceActivator = mockStatic(GuiceActivator.class)) {
            GuiceActivator.bundleContext = bundleContext;

            BundleJarsPrioritizingRunnable.BundleJarsPrioritizingClassLoader classLoader =
                new BundleJarsPrioritizingRunnable.BundleJarsPrioritizingClassLoader(urls, parent, secondary);

            // Act & Assert
            assertThrows(ClassNotFoundException.class, () -> classLoader.findClass("com.test.NonExistentClass"));
        }
    }

    @Test
    void bundleJarsPrioritizingClassLoader_findClass_withClassNameWithoutPackage_shouldSkipBundleSearch() throws ClassNotFoundException {
        // Arrange - Test with a class name that doesn't contain a dot (no package)
        URL[] urls = new URL[0];
        URLClassLoader parent = new URLClassLoader(urls);
        ClassLoader secondary = mock(ClassLoader.class);

        Bundle bundle = mock(Bundle.class);
        Bundle[] bundles = {bundle};

        doThrow(new ClassNotFoundException()).when(secondary).loadClass("ClassWithoutPackage");

        BundleContext bundleContext = mock(BundleContext.class);
        when(bundleContext.getBundles()).thenReturn(bundles);

        try (MockedStatic<GuiceActivator> guiceActivator = mockStatic(GuiceActivator.class)) {
            GuiceActivator.bundleContext = bundleContext;

            BundleJarsPrioritizingRunnable.BundleJarsPrioritizingClassLoader classLoader =
                new BundleJarsPrioritizingRunnable.BundleJarsPrioritizingClassLoader(urls, parent, secondary);

            // Act & Assert
            assertThrows(ClassNotFoundException.class, () -> classLoader.findClass("ClassWithoutPackage"));

            // Verify bundle.getHeaders was never called since the class name doesn't contain a dot
            verify(bundle, never()).getHeaders();
        }
    }

    @Test
    void bundleJarsPrioritizingClassLoader_findClass_withNullExportPackage_shouldSkipBundle() throws ClassNotFoundException {
        // Arrange
        URL[] urls = new URL[0];
        URLClassLoader parent = new URLClassLoader(urls);
        ClassLoader secondary = mock(ClassLoader.class);

        Bundle bundle = mock(Bundle.class);
        Bundle[] bundles = {bundle};

        Dictionary<String, String> headers = new Hashtable<>();
        when(bundle.getHeaders()).thenReturn(headers);
        doThrow(new ClassNotFoundException()).when(secondary).loadClass("com.test.TestClass");

        BundleContext bundleContext = mock(BundleContext.class);
        when(bundleContext.getBundles()).thenReturn(bundles);

        try (MockedStatic<GuiceActivator> guiceActivator = mockStatic(GuiceActivator.class)) {
            GuiceActivator.bundleContext = bundleContext;

            BundleJarsPrioritizingRunnable.BundleJarsPrioritizingClassLoader classLoader =
                new BundleJarsPrioritizingRunnable.BundleJarsPrioritizingClassLoader(urls, parent, secondary);

            // Act & Assert
            assertThrows(ClassNotFoundException.class, () -> classLoader.findClass("com.test.TestClass"));
            verify(bundle, never()).loadClass(anyString());
        }
    }


    @Test
    void bundleJarsPrioritizingClassLoader_findClass_withBundleExceptionDuringLoad_shouldContinueToNextBundle() throws ClassNotFoundException {
        // Arrange
        URL[] urls = new URL[0];
        URLClassLoader parent = new URLClassLoader(urls);
        ClassLoader secondary = mock(ClassLoader.class);

        Bundle bundle1 = mock(Bundle.class);
        Bundle bundle2 = mock(Bundle.class);
        Bundle[] bundles = {bundle1, bundle2};

        Dictionary<String, String> headers1 = new Hashtable<>();
        headers1.put(Constants.EXPORT_PACKAGE, "com.test");
        Dictionary<String, String> headers2 = new Hashtable<>();
        headers2.put(Constants.EXPORT_PACKAGE, "com.test");

        when(bundle1.getHeaders()).thenReturn(headers1);
        when(bundle2.getHeaders()).thenReturn(headers2);

        doThrow(new ClassNotFoundException()).when(secondary).loadClass("com.test.TestClass");
        doThrow(new ClassNotFoundException()).when(bundle1).loadClass("com.test.TestClass");
        doReturn(String.class).when(bundle2).loadClass("com.test.TestClass");

        BundleContext bundleContext = mock(BundleContext.class);
        when(bundleContext.getBundles()).thenReturn(bundles);

        try (MockedStatic<GuiceActivator> guiceActivator = mockStatic(GuiceActivator.class)) {
            GuiceActivator.bundleContext = bundleContext;

            BundleJarsPrioritizingRunnable.BundleJarsPrioritizingClassLoader classLoader =
                new BundleJarsPrioritizingRunnable.BundleJarsPrioritizingClassLoader(urls, parent, secondary);

            // Act
            Class<?> result = classLoader.findClass("com.test.TestClass");

            // Assert
            assertEquals(String.class, result);
            verify(bundle1).loadClass("com.test.TestClass");
            verify(bundle2).loadClass("com.test.TestClass");
        }
    }

    @Test
    void execute_withSerializableParams_shouldSerializeAndExecute() {
        // Arrange
        Map<String, Object> params = Map.of("testKey", "testValue");

        try (MockedStatic<ManifestUtils> manifestUtils = mockStatic(ManifestUtils.class)) {

            // Setup manifest mocking
            Attributes mockAttributes = mock(Attributes.class);
            manifestUtils.when(ManifestUtils::getManifestAttributes).thenReturn(mockAttributes);
            when(mockAttributes.getValue(Constants.BUNDLE_CLASSPATH)).thenReturn(".");

            // Act - This will actually execute with the test class loader
            Map<String, Object> result = BundleJarsPrioritizingRunnable.execute(TestRunnableImpl.class, params);

            // Assert
            assertNotNull(result);
            // We expect the default result since TestRunnableImpl returns Map.of("default", "result")
            assertEquals("result", result.get("default"));
        }
    }

    @Test
    void execute_withNonSerializableParams_shouldPassParamsDirectly() {
        // Arrange
        Map<String, Object> params = Map.of("key", "value");

        try (MockedStatic<ManifestUtils> manifestUtils = mockStatic(ManifestUtils.class);
             MockedStatic<ObjectUtils> objectUtils = mockStatic(ObjectUtils.class)) {

            // Setup manifest mocking
            Attributes mockAttributes = mock(Attributes.class);
            manifestUtils.when(ManifestUtils::getManifestAttributes).thenReturn(mockAttributes);
            when(mockAttributes.getValue(Constants.BUNDLE_CLASSPATH)).thenReturn(".");

            // Mock ObjectUtils.serialize to throw NotSerializableException to simulate non-serializable params
            objectUtils.when(() -> ObjectUtils.serialize(any())).thenThrow(new NotSerializableException("Not serializable"));

            // Act - This should handle the exception and pass params as-is
            Map<String, Object> result = BundleJarsPrioritizingRunnable.execute(TestRunnableImpl.class, params);

            // Assert
            assertNotNull(result);
            assertEquals("result", result.get("default"));
        }
    }

    @Test
    void execute_successfulExecution_shouldInvokeRunMethod() {
        // Arrange
        Map<String, Object> params = Map.of("input", "test");

        try (MockedStatic<ManifestUtils> manifestUtils = mockStatic(ManifestUtils.class)) {
            // Setup manifest mocking
            Attributes mockAttributes = mock(Attributes.class);
            manifestUtils.when(ManifestUtils::getManifestAttributes).thenReturn(mockAttributes);
            when(mockAttributes.getValue(Constants.BUNDLE_CLASSPATH)).thenReturn(".");

            // Act
            Map<String, Object> result = BundleJarsPrioritizingRunnable.execute(TestRunnableImpl.class, params);

            // Assert
            assertNotNull(result);
            assertFalse(result.containsKey(BundleJarsPrioritizingRunnable.ERROR_KEY));
        }
    }

    @Test
    void execute_shouldRestoreContextClassLoader() {
        // Arrange
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Map<String, Object> params = Map.of("key", "value");

        try (MockedStatic<ManifestUtils> manifestUtils = mockStatic(ManifestUtils.class)) {
            // Setup manifest mocking
            Attributes mockAttributes = mock(Attributes.class);
            manifestUtils.when(ManifestUtils::getManifestAttributes).thenReturn(mockAttributes);
            when(mockAttributes.getValue(Constants.BUNDLE_CLASSPATH)).thenReturn(".");

            // Act
            BundleJarsPrioritizingRunnable.execute(TestRunnableImpl.class, params);

            // Assert - class loader should be restored
            assertEquals(originalClassLoader, Thread.currentThread().getContextClassLoader());
        }
    }

    @Test
    void execute_withoutPluginXml_shouldExecuteSuccessfully() {
        // Arrange - Use a class that doesn't have plugin.xml in its resources
        Map<String, Object> params = Map.of("key", "value");

        try (MockedStatic<ManifestUtils> manifestUtils = mockStatic(ManifestUtils.class)) {
            // Setup manifest mocking
            Attributes mockAttributes = mock(Attributes.class);
            manifestUtils.when(ManifestUtils::getManifestAttributes).thenReturn(mockAttributes);
            when(mockAttributes.getValue(Constants.BUNDLE_CLASSPATH)).thenReturn(".");

            // Act - This should succeed even without plugin.xml
            Map<String, Object> result = BundleJarsPrioritizingRunnable.execute(RunnableWithoutPluginXml.class, params);

            // Assert
            assertNotNull(result);
            assertEquals("no plugin xml", result.get("result"));
        }
    }


    // executeCached tests

    @Test
    void executeCached_shouldExecuteSuccessfully() {
        // Arrange
        Map<String, Object> params = Map.of("key", "value");

        try (MockedStatic<ManifestUtils> manifestUtils = mockStatic(ManifestUtils.class)) {
            Attributes mockAttributes = mock(Attributes.class);
            manifestUtils.when(ManifestUtils::getManifestAttributes).thenReturn(mockAttributes);
            when(mockAttributes.getValue(Constants.BUNDLE_CLASSPATH)).thenReturn(".");

            // Act
            Map<String, Object> result = BundleJarsPrioritizingRunnable.executeCached(TestRunnableImpl.class, params);

            // Assert
            assertNotNull(result);
            assertEquals("result", result.get("default"));
        }
    }

    @Test
    void executeCached_shouldReuseCache() {
        // Arrange
        Map<String, Object> params = Map.of("key", "value");

        try (MockedStatic<ManifestUtils> manifestUtils = mockStatic(ManifestUtils.class)) {
            Attributes mockAttributes = mock(Attributes.class);
            manifestUtils.when(ManifestUtils::getManifestAttributes).thenReturn(mockAttributes);
            when(mockAttributes.getValue(Constants.BUNDLE_CLASSPATH)).thenReturn(".");

            // Act - call twice
            Map<String, Object> result1 = BundleJarsPrioritizingRunnable.executeCached(CacheTestRunnable.class, params);
            Map<String, Object> result2 = BundleJarsPrioritizingRunnable.executeCached(CacheTestRunnable.class, params);

            // Assert - ManifestUtils should only be called once due to caching
            manifestUtils.verify(ManifestUtils::getManifestAttributes, times(1));
            assertNotNull(result1);
            assertNotNull(result2);
        }
    }

    @Test
    void executeCached_withException_shouldReturnErrorMap() {
        // Arrange
        Map<String, Object> params = Map.of("key", "value");

        try (MockedStatic<ManifestUtils> manifestUtils = mockStatic(ManifestUtils.class)) {
            RuntimeException expectedException = new RuntimeException("Test exception");
            manifestUtils.when(ManifestUtils::getManifestAttributes).thenThrow(expectedException);

            // Act
            Map<String, Object> result = BundleJarsPrioritizingRunnable.executeCached(ExceptionCacheTestRunnable.class, params);

            // Assert
            assertNotNull(result);
            assertTrue(result.containsKey(BundleJarsPrioritizingRunnable.ERROR_KEY));
        }
    }

    @Test
    void executeCached_withExceptionAndRethrowTrue_shouldThrowException() {
        // Arrange
        Map<String, Object> params = Map.of("key", "value");

        try (MockedStatic<ManifestUtils> manifestUtils = mockStatic(ManifestUtils.class)) {
            RuntimeException expectedException = new RuntimeException("Test exception");
            manifestUtils.when(ManifestUtils::getManifestAttributes).thenThrow(expectedException);

            // Act & Assert
            assertThrows(RuntimeException.class,
                () -> BundleJarsPrioritizingRunnable.executeCached(RethrowCacheTestRunnable.class, params, true));
        }
    }

    @Test
    void executeCached_shouldRestoreContextClassLoader() {
        // Arrange
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Map<String, Object> params = Map.of("key", "value");

        try (MockedStatic<ManifestUtils> manifestUtils = mockStatic(ManifestUtils.class)) {
            Attributes mockAttributes = mock(Attributes.class);
            manifestUtils.when(ManifestUtils::getManifestAttributes).thenReturn(mockAttributes);
            when(mockAttributes.getValue(Constants.BUNDLE_CLASSPATH)).thenReturn(".");

            // Act
            BundleJarsPrioritizingRunnable.executeCached(ClassLoaderCacheTestRunnable.class, params);

            // Assert
            assertEquals(originalClassLoader, Thread.currentThread().getContextClassLoader());
        }
    }

    @Test
    void executeCached_withSerializableParams_shouldSerializeAndExecute() {
        // Arrange
        Map<String, Object> params = Map.of("testKey", "testValue");

        try (MockedStatic<ManifestUtils> manifestUtils = mockStatic(ManifestUtils.class)) {
            Attributes mockAttributes = mock(Attributes.class);
            manifestUtils.when(ManifestUtils::getManifestAttributes).thenReturn(mockAttributes);
            when(mockAttributes.getValue(Constants.BUNDLE_CLASSPATH)).thenReturn(".");

            // Act
            Map<String, Object> result = BundleJarsPrioritizingRunnable.executeCached(SerializableCacheTestRunnable.class, params);

            // Assert
            assertNotNull(result);
            assertEquals("result", result.get("default"));
        }
    }

    @Test
    void executeCached_withNonSerializableParams_shouldPassParamsDirectly() {
        // Arrange
        Map<String, Object> params = Map.of("key", "value");

        try (MockedStatic<ManifestUtils> manifestUtils = mockStatic(ManifestUtils.class);
             MockedStatic<ObjectUtils> objectUtils = mockStatic(ObjectUtils.class)) {

            Attributes mockAttributes = mock(Attributes.class);
            manifestUtils.when(ManifestUtils::getManifestAttributes).thenReturn(mockAttributes);
            when(mockAttributes.getValue(Constants.BUNDLE_CLASSPATH)).thenReturn(".");

            // Mock ObjectUtils.serialize to throw NotSerializableException
            objectUtils.when(() -> ObjectUtils.serialize(any())).thenThrow(new NotSerializableException("Not serializable"));

            // Act
            Map<String, Object> result = BundleJarsPrioritizingRunnable.executeCached(NonSerializableCacheTestRunnable.class, params);

            // Assert
            assertNotNull(result);
            assertEquals("result", result.get("default"));
        }
    }

    @Test
    void executeCached_shouldRestoreContextClassLoaderAfterException() {
        // Arrange
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Map<String, Object> params = Map.of("key", "value");

        try (MockedStatic<ManifestUtils> manifestUtils = mockStatic(ManifestUtils.class)) {
            manifestUtils.when(ManifestUtils::getManifestAttributes).thenThrow(new RuntimeException("Test"));

            // Act
            BundleJarsPrioritizingRunnable.executeCached(ExceptionClassLoaderCacheTestRunnable.class, params);

            // Assert
            assertEquals(originalClassLoader, Thread.currentThread().getContextClassLoader());
        }
    }

    // createRunner exception handling test

    @Test
    void createRunner_shouldCloseClassLoaderOnException() {
        // Arrange
        Map<String, Object> params = Map.of("key", "value");

        try (MockedStatic<ManifestUtils> manifestUtils = mockStatic(ManifestUtils.class)) {
            Attributes mockAttributes = mock(Attributes.class);
            manifestUtils.when(ManifestUtils::getManifestAttributes).thenReturn(mockAttributes);
            when(mockAttributes.getValue(Constants.BUNDLE_CLASSPATH)).thenReturn(".");

            // Act - using abstract class should fail during instantiation
            Map<String, Object> result = BundleJarsPrioritizingRunnable.executeCached(AbstractRunnableImpl.class, params);

            // Assert - should return error and classloader should be closed (no resource leak)
            assertNotNull(result);
            assertTrue(result.containsKey(BundleJarsPrioritizingRunnable.ERROR_KEY));
        }
    }

    // collectBundleURLs tests

    @Test
    void execute_withPluginXml_shouldAddBundleRootUrl() {
        // Arrange
        Map<String, Object> params = Map.of("key", "value");

        try (MockedStatic<ManifestUtils> manifestUtils = mockStatic(ManifestUtils.class)) {
            Attributes mockAttributes = mock(Attributes.class);
            manifestUtils.when(ManifestUtils::getManifestAttributes).thenReturn(mockAttributes);
            when(mockAttributes.getValue(Constants.BUNDLE_CLASSPATH)).thenReturn(".");

            // Act - TestRunnableImpl class has access to plugin.xml in test resources
            Map<String, Object> result = BundleJarsPrioritizingRunnable.execute(RunnableWithPluginXml.class, params);

            // Assert
            assertNotNull(result);
            // The execution should succeed, meaning plugin.xml URL was properly added
            assertFalse(result.containsKey(BundleJarsPrioritizingRunnable.ERROR_KEY));
        }
    }

    // Tests for real object serialization/deserialization

    @Test
    void execute_withComplexSerializableObject_shouldPassObjectCorrectly() {
        // Arrange - create a complex serializable object
        TestSerializableObject.TestNestedObject nested1 = TestSerializableObject.TestNestedObject.builder()
                .name("nested1")
                .value(100)
                .tags(List.of("tag1", "tag2"))
                .build();

        TestSerializableObject.TestNestedObject nested2 = TestSerializableObject.TestNestedObject.builder()
                .name("nested2")
                .value(200)
                .tags(List.of("tag3"))
                .build();

        TestSerializableObject testObject = TestSerializableObject.builder()
                .stringField("testString")
                .intField(42)
                .integerField(100)
                .longField(999L)
                .doubleField(3.14)
                .booleanField(true)
                .stringList(List.of("a", "b", "c"))
                .integerSet(Set.of(1, 2, 3))
                .nestedMap(Map.of("key1", "value1", "key2", 123))
                .localDate(LocalDate.of(2024, 6, 15))
                .localDateTime(LocalDateTime.of(2024, 6, 15, 10, 30, 0))
                .nestedObject(nested1)
                .nestedObjectList(List.of(nested1, nested2))
                .build();

        Map<String, Object> params = new HashMap<>();
        params.put("testObject", testObject);
        params.put("simpleString", "hello");

        try (MockedStatic<ManifestUtils> manifestUtils = mockStatic(ManifestUtils.class)) {
            Attributes mockAttributes = mock(Attributes.class);
            manifestUtils.when(ManifestUtils::getManifestAttributes).thenReturn(mockAttributes);
            when(mockAttributes.getValue(Constants.BUNDLE_CLASSPATH)).thenReturn(".");

            // Act
            Map<String, Object> result = BundleJarsPrioritizingRunnable.execute(ComplexObjectCapturingRunnable.class, params);

            // Assert
            assertNotNull(result);
            assertFalse(result.containsKey(BundleJarsPrioritizingRunnable.ERROR_KEY));

            TestSerializableObject received = (TestSerializableObject) result.get("receivedObject");
            assertNotNull(received);
            assertEquals("testString", received.getStringField());
            assertEquals(42, received.getIntField());
            assertEquals(100, received.getIntegerField());
            assertEquals(999L, received.getLongField());
            assertEquals(3.14, received.getDoubleField(), 0.001);
            assertTrue(received.isBooleanField());
            assertEquals(List.of("a", "b", "c"), received.getStringList());
            assertEquals(Set.of(1, 2, 3), received.getIntegerSet());
            assertEquals(LocalDate.of(2024, 6, 15), received.getLocalDate());
            assertEquals(LocalDateTime.of(2024, 6, 15, 10, 30, 0), received.getLocalDateTime());

            assertNotNull(received.getNestedObject());
            assertEquals("nested1", received.getNestedObject().getName());
            assertEquals(100, received.getNestedObject().getValue());

            assertNotNull(received.getNestedObjectList());
            assertEquals(2, received.getNestedObjectList().size());

            assertEquals("hello", result.get("receivedString"));
        }
    }

    @Test
    void executeCached_withComplexSerializableObject_shouldPassObjectCorrectly() {
        // Arrange
        TestSerializableObject testObject = TestSerializableObject.builder()
                .stringField("cachedTest")
                .intField(77)
                .booleanField(false)
                .stringList(List.of("x", "y"))
                .build();

        Map<String, Object> params = new HashMap<>();
        params.put("testObject", testObject);

        try (MockedStatic<ManifestUtils> manifestUtils = mockStatic(ManifestUtils.class)) {
            Attributes mockAttributes = mock(Attributes.class);
            manifestUtils.when(ManifestUtils::getManifestAttributes).thenReturn(mockAttributes);
            when(mockAttributes.getValue(Constants.BUNDLE_CLASSPATH)).thenReturn(".");

            // Act
            Map<String, Object> result = BundleJarsPrioritizingRunnable.executeCached(CachedComplexObjectCapturingRunnable.class, params);

            // Assert
            assertNotNull(result);
            assertFalse(result.containsKey(BundleJarsPrioritizingRunnable.ERROR_KEY));

            TestSerializableObject received = (TestSerializableObject) result.get("receivedObject");
            assertNotNull(received);
            assertEquals("cachedTest", received.getStringField());
            assertEquals(77, received.getIntField());
            assertFalse(received.isBooleanField());
            assertEquals(List.of("x", "y"), received.getStringList());
        }
    }

    @Test
    void execute_withNonSerializableObject_shouldFallbackToDirectPassing() {
        // Arrange - use a non-serializable object (Thread is not serializable)
        Map<String, Object> params = new HashMap<>();
        params.put("thread", Thread.currentThread());
        params.put("string", "test");

        try (MockedStatic<ManifestUtils> manifestUtils = mockStatic(ManifestUtils.class)) {
            Attributes mockAttributes = mock(Attributes.class);
            manifestUtils.when(ManifestUtils::getManifestAttributes).thenReturn(mockAttributes);
            when(mockAttributes.getValue(Constants.BUNDLE_CLASSPATH)).thenReturn(".");

            // Act - should fall back to passing params directly
            Map<String, Object> result = BundleJarsPrioritizingRunnable.execute(NonSerializableObjectRunnable.class, params);

            // Assert - the runnable should still receive the params (passed directly, not serialized)
            assertNotNull(result);
            assertFalse(result.containsKey(BundleJarsPrioritizingRunnable.ERROR_KEY));
            assertEquals("received", result.get("status"));
            assertNotNull(result.get("receivedThread"));
        }
    }

    @Test
    void runInternal_withMapParams_shouldCallRunDirectly() {
        // Arrange
        Map<String, Object> params = Map.of("key", "value");
        Map<String, Object> expectedResult = Map.of("result", "success");
        TestRunnableImpl runnable = new TestRunnableImpl(expectedResult);

        // Act
        Map<String, Object> result = runnable.run(params);

        // Assert
        assertEquals(expectedResult, result);
        assertEquals(params, runnable.getReceivedParams());
    }

    @Test
    void runInternal_withByteArrayParams_shouldDeserializeAndCallRun() {
        // Arrange
        Map<String, Object> expectedParams = Map.of("key", "value");
        Map<String, Object> expectedResult = Map.of("result", "success");
        TestRunnableImpl runnable = new TestRunnableImpl(expectedResult);
        byte[] serializedParams = new byte[]{1, 2, 3};

        try (MockedStatic<ObjectUtils> objectUtils = mockStatic(ObjectUtils.class)) {
            objectUtils.when(() -> ObjectUtils.deserialize(serializedParams)).thenReturn(expectedParams);

            // Act
            Map<String, Object> result = runnable.runInternal(serializedParams);

            // Assert
            assertEquals(expectedResult, result);
            assertEquals(expectedParams, runnable.getReceivedParams());
            objectUtils.verify(() -> ObjectUtils.deserialize(serializedParams));
        }
    }

    // Test helper classes
    @Getter
    public static class TestRunnableImpl implements BundleJarsPrioritizingRunnable {
        private final Map<String, Object> result;
        private Map<String, Object> receivedParams;

        public TestRunnableImpl() {
            this.result = Map.of("default", "result");
        }

        public TestRunnableImpl(Map<String, Object> result) {
            this.result = result;
        }

        @Override
        public Map<String, Object> run(Map<String, Object> params) {
            this.receivedParams = params;
            return result;
        }
    }

    @Getter
    public static class NonSerializableTestRunnableImpl implements BundleJarsPrioritizingRunnable {
        private final Map<String, Object> result;
        private Map<String, Object> receivedParams;
        private boolean runInternalWithMapCalled = false;
        private boolean runInternalWithBytesCalled = false;

        public NonSerializableTestRunnableImpl() {
            this.result = Map.of("default", "result");
        }

        public NonSerializableTestRunnableImpl(Map<String, Object> result) {
            this.result = result;
        }

        @Override
        public Map<String, Object> run(Map<String, Object> params) {
            this.runInternalWithMapCalled = true;
            return result;
        }

        @Override
        public Map<String, Object> runInternal(byte[] paramsSerialized) {
            this.runInternalWithBytesCalled = true;
            return BundleJarsPrioritizingRunnable.super.runInternal(paramsSerialized);
        }
    }

    public abstract static class AbstractRunnableImpl implements BundleJarsPrioritizingRunnable {
        @Override
        public Map<String, Object> run(Map<String, Object> params) {
            return Map.of("result", "abstract");
        }
    }

    public interface NonExistentRunnable extends BundleJarsPrioritizingRunnable {
    }

    public static class RunnableWithoutPluginXml implements BundleJarsPrioritizingRunnable {
        @Override
        public Map<String, Object> run(Map<String, Object> params) {
            return Map.of("result", "no plugin xml");
        }
    }

    // Additional test helper classes for executeCached tests
    public static class CacheTestRunnable implements BundleJarsPrioritizingRunnable {
        @Override
        public Map<String, Object> run(Map<String, Object> params) {
            return Map.of("cached", "result");
        }
    }

    public static class ExceptionCacheTestRunnable implements BundleJarsPrioritizingRunnable {
        @Override
        public Map<String, Object> run(Map<String, Object> params) {
            return Map.of("result", "value");
        }
    }

    public static class RethrowCacheTestRunnable implements BundleJarsPrioritizingRunnable {
        @Override
        public Map<String, Object> run(Map<String, Object> params) {
            return Map.of("result", "value");
        }
    }

    public static class ClassLoaderCacheTestRunnable implements BundleJarsPrioritizingRunnable {
        @Override
        public Map<String, Object> run(Map<String, Object> params) {
            return Map.of("result", "value");
        }
    }

    public static class ExceptionClassLoaderCacheTestRunnable implements BundleJarsPrioritizingRunnable {
        @Override
        public Map<String, Object> run(Map<String, Object> params) {
            return Map.of("result", "value");
        }
    }

    public static class RunnableWithPluginXml implements BundleJarsPrioritizingRunnable {
        @Override
        public Map<String, Object> run(Map<String, Object> params) {
            return Map.of("result", "with plugin xml");
        }
    }

    public static class SerializableCacheTestRunnable implements BundleJarsPrioritizingRunnable {
        @Override
        public Map<String, Object> run(Map<String, Object> params) {
            return Map.of("default", "result");
        }
    }

    public static class NonSerializableCacheTestRunnable implements BundleJarsPrioritizingRunnable {
        @Override
        public Map<String, Object> run(Map<String, Object> params) {
            return Map.of("default", "result");
        }
    }

    public static class ComplexObjectCapturingRunnable implements BundleJarsPrioritizingRunnable {
        @Override
        public Map<String, Object> run(Map<String, Object> params) {
            Map<String, Object> result = new HashMap<>();
            result.put("receivedObject", params.get("testObject"));
            result.put("receivedString", params.get("simpleString"));
            return result;
        }
    }

    public static class CachedComplexObjectCapturingRunnable implements BundleJarsPrioritizingRunnable {
        @Override
        public Map<String, Object> run(Map<String, Object> params) {
            Map<String, Object> result = new HashMap<>();
            result.put("receivedObject", params.get("testObject"));
            return result;
        }
    }

    public static class NonSerializableObjectRunnable implements BundleJarsPrioritizingRunnable {
        @Override
        public Map<String, Object> run(Map<String, Object> params) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "received");
            result.put("receivedThread", params.get("thread"));
            return result;
        }
    }
}
