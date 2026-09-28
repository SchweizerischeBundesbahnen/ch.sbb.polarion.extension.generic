package ch.sbb.polarion.extension.generic.jobs;

import ch.sbb.polarion.extension.generic.rest.filter.LogoutFilter;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.security.ISecurityService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.security.auth.Subject;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Runs work in the background and lets the user who started it poll its state and read its result.
 * <p>
 * Create an instance per request, over the one {@link JobsRegistry} of the job kind. An extension usually subclasses it
 * with a typed {@code startJob} that builds the {@link JobTask}.
 * <p>
 * A job runs as the user who started it: the subject of the start request goes along to the worker thread. A job
 * belongs to that user; for anybody else it does not exist. If the start request asked {@link LogoutFilter} to keep
 * its session alive, the job ends that session when it is over.
 *
 * @param <P> type of the payload stored with each job, for example its parameters
 * @param <R> type of the job result
 */
public class AsyncJobsService<P, R> {

    public static final String CANCELLED_BY_USER_MESSAGE = "Cancelled by user";

    private static final Logger logger = Logger.getLogger(AsyncJobsService.class);

    private final @NotNull JobsRegistry<P, R> registry;
    private final @NotNull ISecurityService securityService;

    public AsyncJobsService(@NotNull JobsRegistry<P, R> registry, @NotNull ISecurityService securityService) {
        this.registry = registry;
        this.securityService = securityService;
    }

    /**
     * Starts a job. Call it on the thread that serves the start request: the user, the subject and the logout
     * decision are taken from there.
     *
     * @param payload          what to keep with the job, see {@link #getJobPayload(String)}
     * @param timeoutInMinutes how long the job may take, counted from now; see {@link TimeoutPolicy} for what happens then
     * @param task             the work
     * @return the ID to poll the job by
     * @throws RejectedExecutionException if the executor of the registry has no room for another job. A job that is
     *                                    not started ends no session: a caller that asked {@link LogoutFilter} to keep
     *                                    the session must give it back, for example by removing that request attribute
     */
    public @NotNull String startJob(@Nullable P payload, int timeoutInMinutes, @NotNull JobTask<R> task) {
        if (timeoutInMinutes <= 0) {
            throw new IllegalArgumentException("Job timeout must be positive: " + timeoutInMinutes);
        }
        Subject userSubject = securityService.getCurrentSubject();
        boolean logoutRequired = isJobLogoutRequired();
        AsyncJob<P, R> job = new AsyncJob<>(UUID.randomUUID().toString(), securityService.getCurrentUser(), payload);

        try {
            registry.register(job, () -> runJob(job, task, userSubject, logoutRequired, timeoutInMinutes));
        } catch (RejectedExecutionException e) {
            logger.error("%s job is refused: no free place for another job".formatted(registry.getJobName()), e);
            throw e;
        }

        ScheduledFuture<?> deadline = registry.scheduleDeadline(() -> onDeadline(job, timeoutInMinutes), timeoutInMinutes);
        job.getFuture().whenComplete((result, thrown) -> deadline.cancel(false));
        return job.jobId();
    }

    /**
     * Cancels a running job. Does nothing if the job is already over.
     * <p>
     * With {@link TimeoutPolicy#INTERRUPT} the job is cancelled at once and its worker thread is interrupted.
     * With {@link TimeoutPolicy#COOPERATIVE} the job is asked to stop and stays running until it stops; if it
     * still finishes with a result, that result counts.
     */
    public void cancelJob(@NotNull String jobId) {
        AsyncJob<P, R> job = getJob(jobId);
        if (job.getFuture().isDone()) {
            return;
        }
        // recorded before the job is stopped, so the failure the stop causes does not replace it
        job.recordFailure(CANCELLED_BY_USER_MESSAGE);
        job.requestCancel();
        job.requestAbort();
        if (registry.getTimeoutPolicy() == TimeoutPolicy.INTERRUPT) {
            if (!job.getFuture().cancel(true)) {
                job.withdrawFailure(CANCELLED_BY_USER_MESSAGE);
            }
            job.interruptWorker();
        }
    }

    public @NotNull JobState getJobState(@NotNull String jobId) {
        return getJob(jobId).toJobState();
    }

    /**
     * @return the job result, or empty while the job is still running
     * @throws IllegalStateException if the job failed or was cancelled
     */
    public @NotNull Optional<R> getJobResult(@NotNull String jobId) {
        AsyncJob<P, R> job = getJob(jobId);
        if (!job.getFuture().isDone()) {
            return Optional.empty();
        }
        if (job.getFuture().isCompletedExceptionally()) {
            throw new IllegalStateException("%s job was cancelled or failed: %s".formatted(registry.getJobName(), job.toJobState().errorMessage()));
        }
        return Optional.ofNullable(job.getFuture().getNow(null));
    }

    /**
     * @return the payload the job was started with
     */
    public @Nullable P getJobPayload(@NotNull String jobId) {
        return getJob(jobId).getPayload();
    }

    /**
     * @return the states of all jobs of the current user, by job ID
     */
    public @NotNull Map<String, JobState> getAllJobsStates() {
        String currentUser = securityService.getCurrentUser();
        return registry.getJobs().stream()
                // built from the job this listing already holds, not looked up by its ID again: a job the cleaner
                // drops meanwhile would answer the whole listing with a 404
                .filter(job -> isOwnedBy(job, currentUser))
                .collect(Collectors.toMap(AsyncJob::jobId, AsyncJob::toJobState));
    }

    /**
     * @return what the caller of the job is shown: the message of the failure itself, or its class where it carries
     * no message, since an empty reason tells the reader nothing
     */
    protected @NotNull String describeFailure(@NotNull Throwable thrown) {
        Throwable reason = rootReason(thrown);
        String message = reason.getMessage();
        return message == null || message.isBlank() ? reason.getClass().getName() : message;
    }

    /**
     * @return the failure worth showing: a future wraps what was thrown, and the wrapper says only which class it was
     */
    public static @NotNull Throwable rootReason(@NotNull Throwable thrown) {
        Throwable reason = thrown;
        while ((reason instanceof CompletionException || reason instanceof ExecutionException) && reason.getCause() != null) {
            reason = reason.getCause();
        }
        return reason;
    }

    public static @NotNull String timeoutMessage(int timeoutInMinutes) {
        return "Timeout after %d min".formatted(timeoutInMinutes);
    }

    @SuppressWarnings("java:S1181") // any throwable must end the job, or it stays "in progress" forever
    private void runJob(@NotNull AsyncJob<P, R> job, @NotNull JobTask<R> task, @Nullable Subject userSubject,
                        boolean logoutRequired, int timeoutInMinutes) {
        job.attachWorker(Thread.currentThread());
        R result = null;
        Throwable failure = null;
        try {
            result = runAsUser(userSubject, () -> task.run(job));
        } catch (Throwable e) {
            failure = e;
        } finally {
            job.detachWorker();
            logoutIfRequired(userSubject, logoutRequired);
        }

        if (failure == null) {
            if (job.getFuture().complete(result)) {
                // a cancel that came while the job was finishing on its own is too late: the result counts
                job.withdrawFailure(CANCELLED_BY_USER_MESSAGE);
            }
            logger.debug("%s job '%s' is finished".formatted(registry.getJobName(), job.jobId()));
        } else {
            job.recordFailure(job.isAbortRequested() ? timeoutMessage(timeoutInMinutes) : describeFailure(failure));
            logger.error("%s job '%s' failed with error: %s".formatted(registry.getJobName(), job.jobId(), job.toJobState().errorMessage()), failure);
            if (job.isCancelRequested()) {
                job.getFuture().cancel(false);
            } else if (failure instanceof CancellationException) {
                // a task that stops on its own throws this, and a future completed with it reads as cancelled
                job.getFuture().completeExceptionally(new CompletionException(failure));
            } else {
                job.getFuture().completeExceptionally(failure);
            }
        }
    }

    private void onDeadline(@NotNull AsyncJob<P, R> job, int timeoutInMinutes) {
        if (job.getFuture().isDone()) {
            return;
        }
        if (registry.getTimeoutPolicy() == TimeoutPolicy.COOPERATIVE) {
            logger.warn("%s job '%s' has run for %d min and is asked to stop".formatted(registry.getJobName(), job.jobId(), timeoutInMinutes));
            job.requestAbort();
            return;
        }
        String reason = timeoutMessage(timeoutInMinutes);
        job.recordFailure(reason);
        job.requestAbort();
        if (job.getFuture().completeExceptionally(new TimeoutException(reason))) {
            logger.error("%s job '%s' failed with error: %s".formatted(registry.getJobName(), job.jobId(), reason));
            job.interruptWorker();
        } else {
            // the job finished on its own in the meantime
            job.withdrawFailure(reason);
        }
    }

    private @Nullable R runAsUser(@Nullable Subject userSubject, @NotNull PrivilegedAction<R> action) {
        // a job started by a call without a subject runs as it is, the way it would have run on the request thread
        if (userSubject == null) {
            return action.run();
        }
        return securityService.doAsUser(userSubject, action);
    }

    private void logoutIfRequired(@Nullable Subject userSubject, boolean logoutRequired) {
        if (userSubject == null || !logoutRequired) {
            return;
        }
        try {
            securityService.logout(userSubject);
        } catch (RuntimeException e) {
            logger.error("Cannot log out after %s job".formatted(registry.getJobName()), e);
        }
    }

    private @NotNull AsyncJob<P, R> getJob(@NotNull String jobId) {
        AsyncJob<P, R> job = registry.getJob(jobId);
        if (job == null || !isOwnedBy(job, securityService.getCurrentUser())) {
            throw new NoSuchElementException("%s job is unknown: %s".formatted(registry.getJobName(), jobId));
        }
        return job;
    }

    private static boolean isOwnedBy(@NotNull AsyncJob<?, ?> job, @Nullable String user) {
        return job.getUser() != null && Objects.equals(job.getUser(), user);
    }

    /**
     * Whether the job has to end the session of its user when it is over.
     * <p>
     * A call authenticated by an XSRF token shares the session of the Polarion UI it was made from, which is not the
     * job's to end. A call that authenticated itself gets a session of its own, which {@link LogoutFilter} would end
     * with the response of the start request, long before the job is over, unless that request asked to keep it.
     * It is then the job that ends it.
     */
    private static boolean isJobLogoutRequired() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            return false;
        }
        if (requestAttributes.getAttribute(LogoutFilter.XSRF_SKIP_LOGOUT, RequestAttributes.SCOPE_REQUEST) == Boolean.TRUE) {
            return false;
        }
        return requestAttributes.getAttribute(LogoutFilter.ASYNC_SKIP_LOGOUT, RequestAttributes.SCOPE_REQUEST) == Boolean.TRUE;
    }
}
