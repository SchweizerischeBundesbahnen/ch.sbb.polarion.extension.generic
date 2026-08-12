package ch.sbb.polarion.extension.generic.rest.feature;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.FeatureContext;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class PrioritizedFiltersFeatureTest {

    @Priority(Priorities.AUTHENTICATION)
    static class AuthenticationLikeFilter {
    }

    @Priority(Priorities.AUTHORIZATION)
    static class AuthorizationLikeFilter {
    }

    static class UnannotatedFilter {
    }

    @Priority(Priorities.USER)
    static class ExplicitUserFilter {
    }

    @Test
    void configureReturnsTrue() {
        PrioritizedFiltersFeature feature = PrioritizedFiltersFeature.of(Set.of(new AuthenticationLikeFilter()));
        FeatureContext context = mock(FeatureContext.class);

        assertTrue(feature.configure(context));
    }

    @Test
    void eachFilterIsRegisteredWithItsAnnotatedPriority() {
        AuthenticationLikeFilter authentication = new AuthenticationLikeFilter();
        AuthorizationLikeFilter authorization = new AuthorizationLikeFilter();
        PrioritizedFiltersFeature feature = PrioritizedFiltersFeature.of(Set.of(authentication, authorization));
        FeatureContext context = mock(FeatureContext.class);

        feature.configure(context);

        verify(context).register(authentication, Priorities.AUTHENTICATION);
        verify(context).register(authorization, Priorities.AUTHORIZATION);
    }

    @Test
    void filterWithoutPriorityAnnotationIsRegisteredWithUserPriority() {
        UnannotatedFilter filter = new UnannotatedFilter();
        PrioritizedFiltersFeature feature = PrioritizedFiltersFeature.of(Set.of(filter));
        FeatureContext context = mock(FeatureContext.class);

        feature.configure(context);

        verify(context).register(filter, Priorities.USER);
    }

    @Test
    void filtersAreRegisteredInAscendingPriorityOrderRegardlessOfInputOrder() {
        Object authentication = new AuthenticationLikeFilter();
        Object authorization = new AuthorizationLikeFilter();
        Object user = new UnannotatedFilter();
        // Intentionally supply them in a non-priority order.
        PrioritizedFiltersFeature feature = PrioritizedFiltersFeature.of(
                new java.util.LinkedHashSet<>(List.of(user, authorization, authentication)));
        FeatureContext context = mock(FeatureContext.class);

        feature.configure(context);

        InOrder inOrder = inOrder(context);
        inOrder.verify(context).register(authentication, Priorities.AUTHENTICATION);
        inOrder.verify(context).register(authorization, Priorities.AUTHORIZATION);
        inOrder.verify(context).register(user, Priorities.USER);
    }

    @Test
    void samePriorityFiltersAreRegisteredInDeterministicClassNameOrder() {
        Object unannotated = new UnannotatedFilter();     // defaults to USER
        Object explicitUser = new ExplicitUserFilter();   // explicit USER
        PrioritizedFiltersFeature feature = PrioritizedFiltersFeature.of(
                new java.util.LinkedHashSet<>(List.of(unannotated, explicitUser)));
        FeatureContext context = mock(FeatureContext.class);

        feature.configure(context);

        // ExplicitUserFilter sorts before UnannotatedFilter by class name at equal priority.
        InOrder inOrder = inOrder(context);
        inOrder.verify(context).register(explicitUser, Priorities.USER);
        inOrder.verify(context).register(unannotated, Priorities.USER);
    }

    @Test
    void everyRegisteredFilterGetsItsOwnPositivePriority() {
        Object authentication = new AuthenticationLikeFilter();
        Object authorization = new AuthorizationLikeFilter();
        Object user = new UnannotatedFilter();
        PrioritizedFiltersFeature feature = PrioritizedFiltersFeature.of(Set.of(authentication, authorization, user));
        FeatureContext context = mock(FeatureContext.class);

        feature.configure(context);

        // Each filter is registered exactly once with its own positive, distinct rank.
        verify(context).register(authentication, Priorities.AUTHENTICATION);
        verify(context).register(authorization, Priorities.AUTHORIZATION);
        verify(context).register(user, Priorities.USER);
        assertTrue(Priorities.AUTHENTICATION > 0 && Priorities.AUTHORIZATION > 0 && Priorities.USER > 0);
        // Distinct ranks are what make the final filter order deterministic.
        assertEquals(3, Set.of(Priorities.AUTHENTICATION, Priorities.AUTHORIZATION, Priorities.USER).size());
    }

    @Test
    void emptyFilterSetRegistersNothing() {
        PrioritizedFiltersFeature feature = PrioritizedFiltersFeature.of(Set.of());
        FeatureContext context = mock(FeatureContext.class);

        assertTrue(feature.configure(context));
        verifyNoInteractions(context);
    }

    @Test
    void priorityOfReadsAnnotatedValue() {
        assertEquals(Priorities.AUTHENTICATION, PrioritizedFiltersFeature.priorityOf(AuthenticationLikeFilter.class));
        assertEquals(Priorities.AUTHORIZATION, PrioritizedFiltersFeature.priorityOf(AuthorizationLikeFilter.class));
    }

    @Test
    void priorityOfDefaultsToUserWhenAnnotationAbsent() {
        assertEquals(Priorities.USER, PrioritizedFiltersFeature.priorityOf(UnannotatedFilter.class));
    }
}
