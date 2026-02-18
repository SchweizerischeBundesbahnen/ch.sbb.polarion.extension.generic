package ch.sbb.polarion.extension.generic.rest.feature;

import org.glassfish.jersey.internal.inject.AbstractBinder;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Jersey Feature that configures HK2 to honor {@link Singleton}, {@link PostConstruct}
 * and {@link PreDestroy} annotations on resource classes registered via
 * {@link javax.ws.rs.core.Application#getClasses()}.
 * <p>
 * By default, Jersey creates a new instance of each resource class per request
 * without HK2 lifecycle management. This feature explicitly registers HK2 bindings
 * so that:
 * <ul>
 *   <li>{@code @Singleton} classes are instantiated once and reused</li>
 *   <li>{@code @PostConstruct} methods are called after instance creation</li>
 *   <li>{@code @PreDestroy} methods are called on shutdown</li>
 * </ul>
 * <p>
 * Usage:
 * <pre>
 * singletons.add(LifecycleBindingFeature.bindFrom(getClasses()));
 * </pre>
 */
public class LifecycleBindingFeature implements Feature {

    private final Set<Class<?>> classes;

    private LifecycleBindingFeature(Set<Class<?>> classes) {
        this.classes = classes;
    }

    public static LifecycleBindingFeature bindFrom(Set<Class<?>> classes) {
        return new LifecycleBindingFeature(classes);
    }

    @Override
    public boolean configure(FeatureContext context) {
        context.register(new AbstractBinder() {
            @Override
            protected void configure() {
                for (Class<?> clazz : classes) {
                    if (clazz.isAnnotationPresent(Singleton.class)) {
                        bind(clazz).to(clazz).in(Singleton.class);
                    } else if (hasLifecycleAnnotations(clazz)) {
                        bind(clazz).to(clazz);
                    }
                }
            }
        });
        return true;
    }

    static boolean hasLifecycleAnnotations(Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(PostConstruct.class) || method.isAnnotationPresent(PreDestroy.class)) {
                return true;
            }
        }
        return false;
    }
}
