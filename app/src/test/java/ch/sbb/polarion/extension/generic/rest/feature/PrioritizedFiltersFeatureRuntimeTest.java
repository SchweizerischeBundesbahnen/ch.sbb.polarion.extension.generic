package ch.sbb.polarion.extension.generic.rest.feature;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.FeatureContext;
import org.glassfish.jersey.model.internal.RankedComparator;
import org.glassfish.jersey.model.internal.RankedProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Runtime-ordering counterpart to {@link PrioritizedFiltersFeatureTest}. The mocked-{@code FeatureContext} tests
 * assert only that {@link PrioritizedFiltersFeature} <em>registers</em> filters with the right ranks; they do not
 * prove that those ranks actually place authentication before authorization once Jersey orders the provider chain.
 * <p>
 * This test drives {@link PrioritizedFiltersFeature#configure(FeatureContext)} for real, captures the
 * {@code (filter, priority)} pairs it registers, and then feeds those captured ranks to the very comparator Jersey
 * uses for request filters ({@link RankedComparator} with {@link RankedComparator.Order#ASCENDING}). It therefore
 * catches two regressions the mock-only tests miss: (a) {@code configure} dropping the explicit priority — e.g.
 * calling {@code register(filter)} instead of {@code register(filter, priority)} — because the captor would then
 * find no ranked registration, and (b) a rank assignment that keeps registration correct but yields the wrong
 * effective order. Using the real Jersey comparator keeps this independent of a full {@code ApplicationHandler},
 * whose {@code jersey-server} packages are not on this module's test classpath.
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
     * Runs the feature over the given filters, takes the ranks it actually registered, sorts them with the same
     * comparator Jersey uses for request filters, and asserts the authentication filter is ordered first regardless
     * of the incoming order.
     */
    private void assertAuthenticationRunsFirst(ContainerRequestFilter... filtersInInputOrder) {
        Set<Object> filters = new LinkedHashSet<>(List.of(filtersInInputOrder));
        FeatureContext context = mock(FeatureContext.class);

        PrioritizedFiltersFeature.of(filters).configure(context);

        // Capture what the feature actually registered — this ties the assertion to configure()'s behaviour,
        // so dropping the explicit priority would fail here (no ranked registration to capture).
        ArgumentCaptor<Object> filterCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Integer> priorityCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(context, atLeastOnce()).register(filterCaptor.capture(), priorityCaptor.capture());
        List<Object> registeredFilters = filterCaptor.getAllValues();
        List<Integer> registeredPriorities = priorityCaptor.getAllValues();

        List<RankedProvider<ContainerRequestFilter>> providers = new ArrayList<>();
        for (int i = 0; i < registeredFilters.size(); i++) {
            providers.add(new RankedProvider<>((ContainerRequestFilter) registeredFilters.get(i), registeredPriorities.get(i)));
        }
        providers.sort(new RankedComparator<>(RankedComparator.Order.ASCENDING));

        List<Class<?>> orderedTypes = providers.stream()
                .<Class<?>>map(p -> p.getProvider().getClass())
                .toList();
        assertEquals(List.of(AuthenticationFilterStub.class, AuthorizationFilterStub.class), orderedTypes,
                "Jersey's RankedComparator must order the AUTHENTICATION filter before the AUTHORIZATION filter");
    }
}
