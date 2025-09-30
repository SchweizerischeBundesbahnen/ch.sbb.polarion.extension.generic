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
import java.util.Hashtable;
import java.util.Map;
import java.util.jar.Attributes;

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
    void execute_withSerializableParams_shouldSerializeAndExecute() throws Exception {
        // Arrange
        Map<String, Object> params = Map.of("testKey", "testValue");
        Map<String, Object> expectedResult = Map.of("result", "success");
        byte[] serializedParams = ObjectUtils.serialize(params);

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
        // Create a map with a non-serializable object
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
}
