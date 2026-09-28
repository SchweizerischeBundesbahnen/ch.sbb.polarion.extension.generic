package ch.sbb.polarion.extension.generic.jobs;

import org.jetbrains.annotations.NotNull;

/**
 * The work of an asynchronous job. It runs on a worker thread, as the user who started the job.
 *
 * @param <R> type of the job result
 */
@FunctionalInterface
public interface JobTask<R> {

    /**
     * @return the job result; a {@code null} result reads as "no result yet" in {@link AsyncJobsService#getJobResult(String)}
     */
    R run(@NotNull JobControl control);
}
