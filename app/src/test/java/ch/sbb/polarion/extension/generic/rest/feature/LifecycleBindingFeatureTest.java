package ch.sbb.polarion.extension.generic.rest.feature;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.Binding;
import org.glassfish.jersey.internal.inject.ClassBinding;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;
import javax.ws.rs.core.FeatureContext;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LifecycleBindingFeatureTest {

    @Singleton
    static class SingletonController {
    }

    static class RegularController {
    }

    static class PostConstructController {
        @PostConstruct
        void init() {
        }
    }

    static class PreDestroyController {
        @PreDestroy
        void cleanup() {
        }
    }

    static class BothLifecycleController {
        @PostConstruct
        void init() {
        }

        @PreDestroy
        void cleanup() {
        }
    }

    @Singleton
    static class SingletonWithLifecycleController {
        @PostConstruct
        void init() {
        }

        @PreDestroy
        void cleanup() {
        }
    }

    @Test
    void configureReturnsTrueAndRegistersAbstractBinder() {
        LifecycleBindingFeature feature = LifecycleBindingFeature.bindFrom(Set.of(SingletonController.class));
        FeatureContext context = mock(FeatureContext.class);

        boolean result = feature.configure(context);

        assertTrue(result);
        verify(context).register(org.mockito.ArgumentMatchers.any(AbstractBinder.class));
    }

    @Test
    void singletonClassIsBoundWithSingletonScope() {
        Map<Class<?>, Class<? extends Annotation>> bindings = captureBindings(
                Set.of(SingletonController.class, RegularController.class));

        assertEquals(1, bindings.size());
        assertEquals(Singleton.class, bindings.get(SingletonController.class));
    }

    @Test
    void postConstructClassIsBoundWithDefaultScope() {
        Map<Class<?>, Class<? extends Annotation>> bindings = captureBindings(
                Set.of(PostConstructController.class, RegularController.class));

        assertEquals(1, bindings.size());
        assertTrue(bindings.containsKey(PostConstructController.class));
        assertNull(bindings.get(PostConstructController.class));
    }

    @Test
    void preDestroyClassIsBoundWithDefaultScope() {
        Map<Class<?>, Class<? extends Annotation>> bindings = captureBindings(
                Set.of(PreDestroyController.class, RegularController.class));

        assertEquals(1, bindings.size());
        assertTrue(bindings.containsKey(PreDestroyController.class));
        assertNull(bindings.get(PreDestroyController.class));
    }

    @Test
    void bothLifecycleAnnotationsClassIsBound() {
        Map<Class<?>, Class<? extends Annotation>> bindings = captureBindings(
                Set.of(BothLifecycleController.class));

        assertEquals(1, bindings.size());
        assertTrue(bindings.containsKey(BothLifecycleController.class));
        assertNull(bindings.get(BothLifecycleController.class));
    }

    @Test
    void singletonWithLifecycleIsBoundAsSingleton() {
        Map<Class<?>, Class<? extends Annotation>> bindings = captureBindings(
                Set.of(SingletonWithLifecycleController.class));

        assertEquals(1, bindings.size());
        assertEquals(Singleton.class, bindings.get(SingletonWithLifecycleController.class));
    }

    @Test
    void regularClassIsNotBound() {
        Map<Class<?>, Class<? extends Annotation>> bindings = captureBindings(
                Set.of(RegularController.class));

        assertTrue(bindings.isEmpty());
    }

    @Test
    void mixedClassesAreBoundCorrectly() {
        Map<Class<?>, Class<? extends Annotation>> bindings = captureBindings(
                Set.of(SingletonController.class, RegularController.class, PostConstructController.class,
                        PreDestroyController.class, BothLifecycleController.class));

        assertEquals(4, bindings.size());
        assertEquals(Singleton.class, bindings.get(SingletonController.class));
        assertTrue(bindings.containsKey(PostConstructController.class));
        assertTrue(bindings.containsKey(PreDestroyController.class));
        assertTrue(bindings.containsKey(BothLifecycleController.class));
        assertFalse(bindings.containsKey(RegularController.class));
    }

    @Test
    void emptyClassSetProducesNoBindings() {
        Map<Class<?>, Class<? extends Annotation>> bindings = captureBindings(Set.of());

        assertTrue(bindings.isEmpty());
    }

    @Test
    void hasLifecycleAnnotationsDetectsPostConstruct() {
        assertTrue(LifecycleBindingFeature.hasLifecycleAnnotations(PostConstructController.class));
    }

    @Test
    void hasLifecycleAnnotationsDetectsPreDestroy() {
        assertTrue(LifecycleBindingFeature.hasLifecycleAnnotations(PreDestroyController.class));
    }

    @Test
    void hasLifecycleAnnotationsDetectsBoth() {
        assertTrue(LifecycleBindingFeature.hasLifecycleAnnotations(BothLifecycleController.class));
    }

    @Test
    void hasLifecycleAnnotationsReturnsFalseForRegular() {
        assertFalse(LifecycleBindingFeature.hasLifecycleAnnotations(RegularController.class));
    }

    @Test
    void hasLifecycleAnnotationsReturnsFalseForSingletonOnly() {
        assertFalse(LifecycleBindingFeature.hasLifecycleAnnotations(SingletonController.class));
    }

    @SuppressWarnings("rawtypes")
    private Map<Class<?>, Class<? extends Annotation>> captureBindings(Set<Class<?>> classes) {
        LifecycleBindingFeature feature = LifecycleBindingFeature.bindFrom(classes);
        FeatureContext context = mock(FeatureContext.class);
        feature.configure(context);

        ArgumentCaptor<AbstractBinder> captor = ArgumentCaptor.forClass(AbstractBinder.class);
        verify(context).register(captor.capture());

        Collection<Binding> bindings = captor.getValue().getBindings();
        Map<Class<?>, Class<? extends Annotation>> result = new java.util.HashMap<>();
        bindings.stream()
                .filter(ClassBinding.class::isInstance)
                .map(ClassBinding.class::cast)
                .forEach(b -> result.put(b.getService(), b.getScope()));
        return result;
    }
}
