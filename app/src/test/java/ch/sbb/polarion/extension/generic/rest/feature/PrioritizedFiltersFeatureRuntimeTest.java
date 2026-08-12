package ch.sbb.polarion.extension.generic.rest.feature;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import org.glassfish.jersey.model.internal.RankedComparator;
import org.glassfish.jersey.model.internal.RankedProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Runtime-ordering counterpart to {@link PrioritizedFiltersFeatureTest}. The mocked-{@code FeatureContext} tests only
 * assert that {@link PrioritizedFiltersFeature} <em>registers</em> filters with the right ranks; they do not prove
 * that those ranks actually place authentication before authorization once Jersey orders the provider chain.
 * <p>
 * Jersey orders {@link ContainerRequestFilter}s at request time with {@link RankedComparator} using
 * {@link RankedComparator.Order#ASCENDING}. This test feeds that very comparator the ranks
 * {@link PrioritizedFiltersFeature} assigns and asserts the resulting execution order, so a regression that keeps
 * registration correct but yields the wrong effective order (the original bug) is caught. Using the real Jersey
 * ordering component keeps this independent of a full {@code ApplicationHandler}, whose {@code jersey-server}
 * packages are not on this module's test classpath.
 */
class PrioritizedFiltersFeatureRuntimeTest {

    @Priority(Priorities.AUTHENTICATION)
    private static class AuthenticationFilterStub implements ContainerRequestFilter {
        @Override
        public void filter(ContainerRequestContext requestContext) {
            // no-op: only the declared @Priority matters for ordering
        }
    }

    @Priority(Priorities.AUTHORIZATION)
    private static class AuthorizationFilterStub implements ContainerRequestFilter {
        @Override
        public void filter(ContainerRequestContext requestContext) {
            // no-op: only the declared @Priority matters for ordering
        }
    }

    @Test
    void jerseyRanksAuthenticationBeforeAuthorizationWhenAuthorizationListedFirst() {
        // Authorization first — the exact input order that produced authorization-before-authentication before the fix.
        assertAuthenticationRunsFirst(new AuthorizationFilterStub(), new AuthenticationFilterStub());
    }

    @Test
    void jerseyRanksAuthenticationBeforeAuthorizationWhenAuthenticationListedFirst() {
        assertAuthenticationRunsFirst(new AuthenticationFilterStub(), new AuthorizationFilterStub());
    }

    /**
     * Wraps the given filters with the ranks {@link PrioritizedFiltersFeature} assigns, sorts them with the same
     * comparator Jersey uses for request filters, and asserts the authentication filter is ordered first regardless
     * of the incoming order.
     */
    private void assertAuthenticationRunsFirst(ContainerRequestFilter... filtersInInputOrder) {
        List<RankedProvider<ContainerRequestFilter>> providers = new java.util.ArrayList<>();
        for (ContainerRequestFilter filter : filtersInInputOrder) {
            int rank = PrioritizedFiltersFeature.priorityOf(filter.getClass());
            providers.add(new RankedProvider<>(filter, rank));
        }

        providers.sort(new RankedComparator<>(RankedComparator.Order.ASCENDING));

        List<Class<?>> orderedTypes = providers.stream()
                .map(p -> p.getProvider().getClass())
                .collect(java.util.stream.Collectors.toList());
        assertEquals(List.of(AuthenticationFilterStub.class, AuthorizationFilterStub.class), orderedTypes,
                "Jersey's RankedComparator must order the AUTHENTICATION filter before the AUTHORIZATION filter");
    }
}
