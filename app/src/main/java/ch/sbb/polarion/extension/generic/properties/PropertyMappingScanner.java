package ch.sbb.polarion.extension.generic.properties;

import ch.sbb.polarion.extension.generic.properties.mappings.PropertyMapping;
import ch.sbb.polarion.extension.generic.properties.mappings.PropertyMappingDefaultValue;
import ch.sbb.polarion.extension.generic.properties.mappings.PropertyMappingDescription;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import static com.polarion.core.util.StringUtils.isEmpty;

public class PropertyMappingScanner<T extends ExtensionConfiguration>  {

    private final Map<String, PropertyInfo> properties = new ConcurrentHashMap<>();
    private final T configInstance;

    public PropertyMappingScanner(T configInstance) {
        this.configInstance = configInstance;
        if (configInstance == null) {
            throw new IllegalArgumentException("Config instance cannot be null");
        }
        scan(configInstance);
    }

    public String getValue(String propertyName) {
        PropertyInfo info = properties.get(propertyName);
        return ((info != null) && (info.value != null))?
                info.value : GetterFinder.getValue(configInstance, propertyName);
    }

    public String getDefaultValue(String propertyName) {
        PropertyInfo info = properties.get(propertyName);
        return ((info != null) && (info.defaultValue != null))?
                info.defaultValue : GetterFinder.getDefaultValue(configInstance, propertyName);
    }

    public String getDescription(String propertyName) {
        PropertyInfo info = properties.get(propertyName);
        return ((info != null) && (info.description != null))?
                info.description : GetterFinder.getDescription(configInstance, propertyName);
    }

    private void scan(Object configInstance) {
        Class<?> clazz = configInstance.getClass();

        for (Method method : clazz.getMethods()) {
            processAnnotation(method, configInstance, PropertyMapping.class, PropertyInfo::withValue);
            processAnnotation(method, configInstance, PropertyMappingDescription.class, PropertyInfo::withDescription);
            processAnnotation(method, configInstance, PropertyMappingDefaultValue.class, PropertyInfo::withDefaultValue);
        }
    }

    private <A extends Annotation> void processAnnotation(
            Method method,
            Object instance,
            Class<A> annotationClass,
            BiFunction<PropertyInfo, String, PropertyInfo> propertyInfoBuilder
    ) {
        if (method.isAnnotationPresent(annotationClass)) {
            try {
                A annotation = method.getAnnotation(annotationClass);
                String propertyKey = (String) annotationClass.getMethod("value").invoke(annotation);
                if (isEmpty(propertyKey)) {
                    throw new IllegalArgumentException("Property key cannot be empty");
                }
                String result = String.valueOf(method.invoke(instance));
                properties.compute(propertyKey,
                        (k, existingInfo) -> {
                            PropertyInfo propertyInfo = Objects.requireNonNullElseGet(existingInfo, PropertyInfo::new);
                            return propertyInfoBuilder.apply(propertyInfo, result);
                        });
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to get value from annotation " + annotationClass.getSimpleName() + " : " + e.getMessage(), e);
            }
        }
    }

    record PropertyInfo(String value, String description, String defaultValue) {
        public PropertyInfo() {
            this(null, null, null);
        }

        public PropertyInfo withValue(String value) {
            return new PropertyInfo(value, this.description, this.defaultValue);
        }

        public PropertyInfo withDescription(String description) {
            return new PropertyInfo(this.value, description, this.defaultValue);
        }

        public PropertyInfo withDefaultValue(String defaultValue) {
            return new PropertyInfo(this.value, this.description, defaultValue);
        }
    }
}
