package ch.sbb.polarion.extension.generic.rest.feature;

import org.glassfish.jersey.internal.inject.AbstractBinder;

import javax.inject.Singleton;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import java.util.Set;

/**
 * Jersey Feature that configures HK2 to honor {@link Singleton} annotation
 * on resource classes registered via {@link javax.ws.rs.core.Application#getClasses()}.
 * <p>
 * By default, Jersey creates a new instance of each resource class per request,
 * ignoring the {@code @Singleton} annotation. This feature explicitly registers
 * HK2 bindings with singleton scope for annotated classes.
 * <p>
 * Usage:
 * <pre>
 * singletons.add(SingletonBindingFeature.bindFrom(getClasses()));
 * </pre>
 */
public class SingletonBindingFeature implements Feature {

    private final Set<Class<?>> classes;

    private SingletonBindingFeature(Set<Class<?>> classes) {
        this.classes = classes;
    }

    public static SingletonBindingFeature bindFrom(Set<Class<?>> classes) {
        return new SingletonBindingFeature(classes);
    }

    @Override
    public boolean configure(FeatureContext context) {
        context.register(new AbstractBinder() {
            @Override
            protected void configure() {
                classes.stream()
                        .filter(c -> c.isAnnotationPresent(Singleton.class))
                        .forEach(c -> bind(c).to(c).in(Singleton.class));
            }
        });
        return true;
    }
}
