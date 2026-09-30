package ch.sbb.polarion.extension.generic.jobs;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobsPropertiesTest {

    @Test
    void shouldReadTimeouts() {
        JobsProperties jobsProperties = new JobsProperties(JobsPropertiesTest.class, "/jobs/test-jobs.properties");

        assertThat(jobsProperties.getFinishedJobTimeout()).isEqualTo(7);
        assertThat(jobsProperties.getInProgressJobTimeout()).isEqualTo(11);
    }

    @Test
    void shouldFailOnMissingProperty() {
        JobsProperties jobsProperties = new JobsProperties(JobsPropertiesTest.class, "/jobs/incomplete-jobs.properties");

        assertThatThrownBy(jobsProperties::getInProgressJobTimeout)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(JobsProperties.TIMEOUT_IN_PROGRESS_JOBS);
    }

    @Test
    void shouldFailOnMissingFile() {
        assertThatThrownBy(() -> new JobsProperties(JobsPropertiesTest.class, "/jobs/missing.properties"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("/jobs/missing.properties");
    }
}
