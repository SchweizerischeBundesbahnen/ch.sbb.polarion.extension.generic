package ch.sbb.polarion.extension.generic.jobs;

/**
 * What happens to a job that runs longer than its timeout, and to a job its caller cancels.
 */
public enum TimeoutPolicy {

    /**
     * The job is declared over at once: timeout makes it failed, cancel makes it cancelled.
     * Its worker thread is interrupted. Use it for jobs that only read, for example exports.
     */
    INTERRUPT,

    /**
     * The job is asked to stop through {@link JobControl#isAbortRequested()} and stays running until it stops.
     * The timeout counts from the start request, so the wait for a free thread is included.
     * Use it for jobs that write: the caller is never told "failed" while the job still writes.
     */
    COOPERATIVE
}
