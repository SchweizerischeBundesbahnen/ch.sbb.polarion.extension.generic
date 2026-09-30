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
     * @return the job result, never {@code null}: a job which returns {@code null} fails with {@link JobMessages#NO_RESULT}
     */
    R run(@NotNull JobControl control);
}
