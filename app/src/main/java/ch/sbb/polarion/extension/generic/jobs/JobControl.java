package ch.sbb.polarion.extension.generic.jobs;

import org.jetbrains.annotations.NotNull;

/**
 * What a running job can ask about itself and tell its caller.
 */
public interface JobControl {

    /**
     * @return the ID the caller polls the job by
     */
    @NotNull String jobId();

    /**
     * A job run with {@link TimeoutPolicy#COOPERATIVE} must check this at points where it can stop safely.
     *
     * @return {@code true} when the job ran out of time or its caller cancelled it
     */
    boolean isAbortRequested();

    /**
     * Records what the job is doing right now. The caller sees it in {@link JobState#progressMessage()}.
     */
    void reportProgress(@NotNull String message);
}
