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
 * Registers request/response filters with an <em>explicit</em> binding priority read from each filter's
 * {@link Priority} annotation (defaulting to {@link Priorities#USER} when absent).
 * <p>
 * Why this is needed. When filters are registered as instances via {@code Application.getSingletons()},
 * Jersey does not reliably capture their {@code @Priority} into the provider model, so several name-bound
 * filters can end up sharing the same default rank. Jersey's {@code RankedComparator} does <b>not</b>
 * return {@code 0} for equal ranks (it returns {@code ±ordering}), so ties are resolved by the iteration
 * order of the {@link java.util.HashSet} backing {@code getSingletons()} — which differs per application
 * instance and per JVM start. The practical effect was a non-deterministic order between
 * {@code AuthenticationFilter} (authentication) and an authorization filter: sometimes authorization ran
 * first, found no authenticated subject, and failed.
 * <p>
 * Passing an explicit priority through {@link FeatureContext#register(Object, int)} bypasses the fragile
 * annotation capture and guarantees distinct, deterministic ranks, so {@code RankedComparator} orders the
 * filters correctly regardless of registration/iteration order.
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
