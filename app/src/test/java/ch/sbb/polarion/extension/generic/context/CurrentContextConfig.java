package ch.sbb.polarion.extension.generic.context;

import ch.sbb.polarion.extension.generic.properties.ExtensionConfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static ch.sbb.polarion.extension.generic.context.CurrentContextExtension.TEST_EXTENSION;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentContextConfig {
    String value() default TEST_EXTENSION;
    Class<?> extensionConfiguration() default ExtensionConfiguration.class;
}