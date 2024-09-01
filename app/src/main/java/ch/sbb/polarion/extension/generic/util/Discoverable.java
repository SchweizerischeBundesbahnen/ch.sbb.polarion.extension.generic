package ch.sbb.polarion.extension.generic.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class marked by this annotation will be eligible to be discovered dynamically by generic.
 *
 * @see ch.sbb.polarion.extension.generic.util.ContextUtils
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@SuppressWarnings("unused")
public @interface Discoverable {
}
