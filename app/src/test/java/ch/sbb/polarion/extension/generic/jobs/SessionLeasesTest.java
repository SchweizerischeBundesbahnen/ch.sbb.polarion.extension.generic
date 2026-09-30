package ch.sbb.polarion.extension.generic.jobs;

import org.junit.jupiter.api.Test;

import javax.security.auth.Subject;

import static org.assertj.core.api.Assertions.assertThat;

class SessionLeasesTest {

    @Test
    void shouldReleaseLastLeaseOnly() {
        SessionLeases leases = new SessionLeases();
        Subject subject = new Subject();

        leases.acquire(subject);
        leases.acquire(subject);

        assertThat(leases.release(subject)).isFalse();
        assertThat(leases.release(subject)).isTrue();
        assertThat(leases.count(subject)).isZero();
    }

    /**
     * Two sessions whose subjects are equal - here, two subjects without principals - are still two sessions.
     */
    @Test
    void shouldCountSessionsByIdentity() {
        SessionLeases leases = new SessionLeases();
        Subject first = new Subject();
        Subject second = new Subject();
        assertThat(first).isEqualTo(second);

        leases.acquire(first);
        leases.acquire(second);

        assertThat(leases.release(first)).isTrue();
        assertThat(leases.count(second)).isEqualTo(1);
    }

    @Test
    void shouldTreatReleaseWithoutLeaseAsLast() {
        assertThat(new SessionLeases().release(new Subject())).isTrue();
    }
}
