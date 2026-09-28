package ch.sbb.polarion.extension.generic.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NamedDaemonThreadFactoryTest {

    @Test
    void shouldCreateNamedDaemonThreads() {
        NamedDaemonThreadFactory factory = new NamedDaemonThreadFactory("TestThread");

        Thread first = factory.newThread(() -> { });
        Thread second = factory.newThread(() -> { });

        assertThat(first.isDaemon()).isTrue();
        assertThat(first.getName()).isEqualTo("TestThread-1");
        assertThat(second.getName()).isEqualTo("TestThread-2");
    }
}
