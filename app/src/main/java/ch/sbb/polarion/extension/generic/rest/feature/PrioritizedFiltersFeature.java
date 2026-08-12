package ch.sbb.polarion.extension.generic.rest.feature;

import com.polarion.core.util.logging.Logger;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Registers the request/response filter <em>instances</em> returned from {@code getSingletons()} with an
 * explicit, {@link Priority}-derived binding priority (defaulting to {@link Priorities#USER} when the
 * annotation is absent), via {@link FeatureContext#register(Object, int)}.
 * <p>
 * Why this is needed. When a filter is added as a bare instance to {@code Application.getSingletons()},
 * its {@code @Priority} is not reliably reflected in the Jersey provider model, so several name-bound
 * filters can end up with the same default rank. Filters that share a rank are <em>not</em> ordered by
 * priority: in the Jersey shipped with Polarion (jersey-common 3.1.8) {@code RankedComparator} returns
 * {@code ±ordering} rather than {@code 0} for equal ranks, and even if it returned {@code 0} the sort
 * would merely be stable — either way ties fall back to the iteration order of the {@link java.util.HashSet}
 * backing {@code getSingletons()}, which varies per application instance and JVM start. The observed effect
 * was a non-deterministic order between {@code AuthenticationFilter} and a downstream authorization filter:
 * sometimes authorization ran first, found no authenticated subject, and failed.
 * <p>
 * Registering with an explicit priority makes each filter's rank <em>annotation-derived and reproducible</em>,
 * so {@code RankedComparator} orders any two filters that carry <em>different</em> {@code @Priority} values
 * deterministically, regardless of registration/iteration order. The guarantee is between distinct
 * priorities only: filters without {@code @Priority} (e.g. {@code CorsFilter}, {@code LogoutFilter}) all
 * resolve to {@link Priorities#USER} and still tie among themselves — harmless as long as their relative
 * order does not matter (both are outside the authentication→authorization request path).
 * <p>
 * Scope: this only covers filters registered as <em>singletons</em>. Filters contributed as classes
 * ({@code getClasses()}) go through Jersey's standard class-model registration and are not touched here.
 * <p>
 * Downstream contract: {@code AuthenticationFilter} runs at {@link Priorities#AUTHENTICATION}. An extension
 * filter that must run <em>after</em> authentication (e.g. an authorization filter that reads the request
 * subject) must therefore carry a strictly larger priority — annotate it {@code @Priority(Priorities.AUTHORIZATION)}.
 * Do not annotate an authorization filter {@code @Priority(Priorities.AUTHENTICATION)}: that ties with
 * {@code AuthenticationFilter} and reintroduces the non-deterministic ordering this class prevents.
 */
public class PrioritizedFiltersFeature implements Feature {

    private static final Logger logger = Logger.getLogger(PrioritizedFiltersFeature.class);

    private final Set<Object> filters;

    private PrioritizedFiltersFeature(Set<Object> filters) {
        this.filters = filters;
    }

    public static PrioritizedFiltersFeature of(Set<Object> filters) {
        return new PrioritizedFiltersFeature(filters);
    }

    @Override
    public boolean configure(FeatureContext context) {
        // The incoming set has no defined iteration order. Distinct explicit priorities already make the
        // final filter order deterministic (Jersey sorts by rank), so this ordering is not required for
        // correctness; we still register in a stable (priority, class-name) order so the debug log is
        // reproducible and any same-priority ties are registered deterministically.
        List<Object> ordered = filters.stream()
                .sorted(Comparator.comparingInt((Object f) -> priorityOf(f.getClass()))
                        .thenComparing(f -> f.getClass().getName()))
                .toList();
        for (Object filter : ordered) {
            int priority = priorityOf(filter.getClass());
            context.register(filter, priority);
            logger.debug("Registered filter " + filter.getClass().getName() + " with priority=" + priority);
        }
        return true;
    }

    /**
     * Reads the {@link Priority} value declared on the filter class, or {@link Priorities#USER} when the
     * annotation is absent (the JAX-RS default for providers without an explicit priority).
     */
    static int priorityOf(Class<?> filterClass) {
        Priority priority = filterClass.getAnnotation(Priority.class);
        return priority != null ? priority.value() : Priorities.USER;
    }
}
