package ch.sbb.polarion.extension.generic.jobs;

import org.jetbrains.annotations.NotNull;

import javax.security.auth.Subject;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Counts the jobs which use a session kept alive for them, so that only the last of them ends it: two jobs started by
 * one request share its session, and the first to finish must not end it under the other.
 * <p>
 * Keyed by the identity of the subject, not by {@link Subject#equals(Object)}, which holds for any two subjects with the
 * same principals - two sessions of one user among them.
 */
final class SessionLeases {

    private final Map<Subject, Integer> leases = new IdentityHashMap<>();

    /**
     * Takes a lease on the session of the subject, before a job which is to end that session starts.
     */
    synchronized void acquire(@NotNull Subject subject) {
        leases.merge(subject, 1, Integer::sum);
    }

    /**
     * Gives a lease back.
     *
     * @return {@code true} if it was the last lease on the session, which may then be ended
     */
    synchronized boolean release(@NotNull Subject subject) {
        Integer count = leases.get(subject);
        if (count == null || count <= 1) {
            leases.remove(subject);
            return true;
        }
        leases.put(subject, count - 1);
        return false;
    }

    synchronized int count(@NotNull Subject subject) {
        return leases.getOrDefault(subject, 0);
    }
}
