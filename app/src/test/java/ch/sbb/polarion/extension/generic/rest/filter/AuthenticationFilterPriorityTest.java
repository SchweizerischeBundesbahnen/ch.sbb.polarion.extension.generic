package ch.sbb.polarion.extension.generic.rest.filter;

import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.model.ContractProvider;
import org.glassfish.jersey.model.internal.CommonConfig;
import org.glassfish.jersey.model.internal.ComponentBag;
import org.glassfish.jersey.model.internal.RankedComparator;
import org.glassfish.jersey.model.internal.RankedProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression test for the authentication-before-authorization ordering.
 * <p>
 * {@link AuthenticationFilter} carries no business logic that would reveal a wrong filter order, so the
 * guard has to be on the rank itself. Without {@code @Priority} the filter falls back to
 * {@link Priorities#USER} (5000), which is <em>larger</em> than the {@link Priorities#AUTHORIZATION} (2000)
 * that a downstream authorization filter carries - and Jersey orders request filters by ascending rank.
 * The result was authorization running first and blowing up on an unauthenticated request. See the
 * ordering contract on {@code GenericRestApplication#getExtensionFilterSingletons()}.
 * <p>
 * The test reproduces Jersey's own rank computation for a provider registered as an instance through
 * {@code Application#getSingletons()}: {@link CommonConfig} builds the {@link ContractProvider} model,
 * {@code ProviderBinder} turns a priority above {@link ContractProvider#NO_PRIORITY} into the binding rank,
 * and {@link RankedProvider} applies its own {@code @Priority} fallback when the binding was left unranked.
 * The ordering is then checked with the very comparator Jersey uses for request filters. Driving a full
 * {@code ApplicationHandler} is not an option here - {@code jersey-server} is not on this module's test
 * classpath.
 */
@ExtendWith(PlatformContextMockExtension.class)
class AuthenticationFilterPriorityTest {

    /** Stand-in for a downstream authorization filter, e.g. the exporters' {@code RolesRestrictedFilter}. */
    @Priority(Priorities.AUTHORIZATION)
    static class AuthorizationFilterStub implements ContainerRequestFilter {
        @Override
        public void filter(ContainerRequestContext requestContext) {
            // ordering probe only
        }
    }

    /**
     * The rank a filter actually gets when it is registered as a bare instance, following Jersey's own path.
     */
    private static int bareSingletonRank(Object filter) {
        CommonConfig config = new CommonConfig(RuntimeType.SERVER, ComponentBag.INCLUDE_ALL);
        config.register(filter);
        int modelPriority = config.getComponentBag().getModel(filter.getClass()).getPriority(ContainerRequestFilter.class);
        int bindingRank = modelPriority > ContractProvider.NO_PRIORITY ? modelPriority : 0;
        return new RankedProvider<>(filter, bindingRank).getRank();
    }

    private static List<Class<?>> effectiveOrder(Set<Object> filters) {
        List<RankedProvider<ContainerRequestFilter>> providers = new ArrayList<>();
        filters.forEach(f -> providers.add(new RankedProvider<>((ContainerRequestFilter) f, bareSingletonRank(f))));
        List<Class<?>> order = new ArrayList<>();
        Providers.sortRankedProviders(new RankedComparator<>(RankedComparator.Order.ASCENDING), providers)
                .forEach(f -> order.add(f.getClass()));
        return order;
    }

    @Test
    void authenticationFilterRunsAtTheAuthenticationPriority() {
        assertEquals(Priorities.AUTHENTICATION, bareSingletonRank(new AuthenticationFilter()));
    }

    @Test
    void authenticationIsOrderedBeforeAuthorizationWhateverTheRegistrationOrder() {
        List<Class<?>> expected = List.of(AuthenticationFilter.class, AuthorizationFilterStub.class);
        assertEquals(expected, effectiveOrder(new LinkedHashSet<>(
                List.of(new AuthenticationFilter(), new AuthorizationFilterStub()))));
        assertEquals(expected, effectiveOrder(new LinkedHashSet<>(
                List.of(new AuthorizationFilterStub(), new AuthenticationFilter()))));
    }
}
