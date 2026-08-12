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
import static org.mockito.Mockito.verifyNoMoreInteractions;

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
        // Every permutation of the input order must yield the same ascending-by-priority registration order.
        List<List<Object>> inputOrders = List.of(
                List.of(authentication, authorization, user),
                List.of(authentication, user, authorization),
                List.of(authorization, authentication, user),
                List.of(authorization, user, authentication),
                List.of(user, authentication, authorization),
                List.of(user, authorization, authentication));

        for (List<Object> inputOrder : inputOrders) {
            FeatureContext context = mock(FeatureContext.class);

            PrioritizedFiltersFeature.of(new java.util.LinkedHashSet<>(inputOrder)).configure(context);

            InOrder inOrder = inOrder(context);
            inOrder.verify(context).register(authentication, Priorities.AUTHENTICATION);
            inOrder.verify(context).register(authorization, Priorities.AUTHORIZATION);
            inOrder.verify(context).register(user, Priorities.USER);
        }
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
    void everyFilterIsRegisteredExactlyOnceWithItsPriority() {
        Object authentication = new AuthenticationLikeFilter();
        Object authorization = new AuthorizationLikeFilter();
        Object user = new UnannotatedFilter();
        PrioritizedFiltersFeature feature = PrioritizedFiltersFeature.of(Set.of(authentication, authorization, user));
        FeatureContext context = mock(FeatureContext.class);

        feature.configure(context);

        verify(context).register(authentication, Priorities.AUTHENTICATION);
        verify(context).register(authorization, Priorities.AUTHORIZATION);
        verify(context).register(user, Priorities.USER);
        // Nothing dropped, nothing registered twice: the three calls above are the only interactions.
        verifyNoMoreInteractions(context);
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
