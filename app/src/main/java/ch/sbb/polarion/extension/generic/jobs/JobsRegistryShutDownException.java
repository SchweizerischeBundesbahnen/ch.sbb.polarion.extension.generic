package ch.sbb.polarion.extension.generic.jobs;

import java.util.concurrent.RejectedExecutionException;

/**
 * A job was refused because its extension is stopping. Unlike a refusal for lack of room, trying again soon does not
 * help, so a REST call answers it with 503.
 */
public class JobsRegistryShutDownException extends RejectedExecutionException {

    public JobsRegistryShutDownException(String message) {
        super(message);
    }
}
