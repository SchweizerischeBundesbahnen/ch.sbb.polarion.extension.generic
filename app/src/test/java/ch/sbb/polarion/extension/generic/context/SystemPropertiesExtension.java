package ch.sbb.polarion.extension.generic.context;

import ch.sbb.polarion.extension.generic.properties.SystemProperties;
import com.polarion.core.config.impl.SystemValueReader;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.MockedStatic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Properties;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class SystemPropertiesExtension implements BeforeEachCallback, AfterEachCallback {

    private MockedStatic<SystemProperties> systemPropertiesMockedStatic;
    private MockedStatic<SystemValueReader> systemValueReaderMockedStatic;

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        systemPropertiesMockedStatic = mockStatic(SystemProperties.class);
        systemValueReaderMockedStatic = mockStatic(SystemValueReader.class);

        Properties properties = getAnnotatedRuntimeProperties(context);
        systemPropertiesMockedStatic.when(SystemProperties::getProperties).thenReturn(properties);

        SystemValueReader systemValueReader = mock(SystemValueReader.class);
        lenient().when(systemValueReader.readString(anyString(), anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return properties.getProperty(key);
        });
        systemValueReaderMockedStatic.when(SystemValueReader::getInstance).thenReturn(systemValueReader);
    }

    private @NotNull Properties getAnnotatedRuntimeProperties(ExtensionContext context) {
        Properties properties = new Properties();

        Method testMethod = context.getRequiredTestMethod();

        if (testMethod.isAnnotationPresent(RuntimeProperties.class)) {
            RuntimeProperties annotation = testMethod.getAnnotation(RuntimeProperties.class);
            if (annotation != null) {
                for (String property : annotation.value()) {
                    String[] keyValue = property.split("=");
                    if (keyValue.length == 2) {
                        properties.setProperty(keyValue[0], keyValue[1]);
                    }
                }
            }
        }
        return properties;
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (systemPropertiesMockedStatic != null) {
            systemPropertiesMockedStatic.close();
        }
        if (systemValueReaderMockedStatic != null) {
            systemValueReaderMockedStatic.close();
        }
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RuntimeProperties {
        String[] value() default {};
    }
}
