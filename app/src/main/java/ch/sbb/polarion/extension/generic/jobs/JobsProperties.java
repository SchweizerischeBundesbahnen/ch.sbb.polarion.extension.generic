package ch.sbb.polarion.extension.generic.jobs;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Job timeouts read from a properties resource of the extension.
 */
public class JobsProperties {
    public static final String TIMEOUT_FINISHED_JOBS = "jobs.timeout.finished.minutes";
    public static final String TIMEOUT_IN_PROGRESS_JOBS = "jobs.timeout.in-progress.minutes";

    private final Properties properties;

    /**
     * @param anchor       class whose class loader finds the resource
     * @param resourcePath path of the resource, for example {@code /pdf-converter-jobs.properties}
     */
    public JobsProperties(@NotNull Class<?> anchor, @NotNull String resourcePath) {
        properties = loadProperties(anchor, resourcePath);
    }

    /**
     * @return how many minutes a job may run
     */
    public int getInProgressJobTimeout() {
        return getIntProperty(TIMEOUT_IN_PROGRESS_JOBS);
    }

    /**
     * @return how many minutes a finished job and its result are kept
     */
    public int getFinishedJobTimeout() {
        return getIntProperty(TIMEOUT_FINISHED_JOBS);
    }

    private int getIntProperty(@NotNull String propName) {
        String propValue = properties.getProperty(propName);
        if (propValue == null) {
            throw new IllegalStateException("Missing property: " + propName);
        }
        return Integer.parseInt(propValue.trim());
    }

    private static @NotNull Properties loadProperties(@NotNull Class<?> anchor, @NotNull String resourcePath) {
        try (InputStream propsInputStream = anchor.getResourceAsStream(resourcePath)) {
            if (propsInputStream == null) {
                throw new IllegalStateException("Properties file is not found: " + resourcePath);
            }
            Properties props = new Properties();
            props.load(propsInputStream);
            return props;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load properties file: " + resourcePath, e);
        }
    }
}
