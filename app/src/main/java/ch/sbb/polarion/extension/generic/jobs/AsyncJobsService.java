package ch.sbb.polarion.extension.generic.jobs;

import ch.sbb.polarion.extension.generic.rest.filter.LogoutFilter;
import ch.sbb.polarion.extension.generic.rest.model.jobs.JobStatus;
import ch.sbb.polarion.extension.generic.util.RequestContextUtil;
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
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

/**
 * Runs work in the background and lets the user who started it poll its state and read its result.
 * <p>
 * Create an instance per request, over the one {@link JobsRegistry} of the job kind. An extension usually subclasses it
 * with a typed {@code startJob} that builds the {@link JobTask}.
 * <p>
 * A job runs as the user who started it: the subject of the start request goes along to the worker thread. A job
 * belongs to that user; for anybody else it does not exist. If the start request asked {@link LogoutFilter} to keep
 * its session alive, that session is ended once, when the job is over: by the worker which ran it, or by whoever ended
 * it before it got a thread. If one request starts several jobs, they share its session, and the last of them to end
 * ends it.
 * <p>
 * A job ends once, by whoever ends it first: its worker with a result or a failure, or a stop - its deadline, a cancel,
 * a shutdown of the registry. What a stop does depends on the {@link TimeoutPolicy} of the registry.
 *
 * @param <P> type of the payload stored with each job, for example its parameters
 * @param <R> type of the job result
 */
public class AsyncJobsService<P, R> {

    private static final Logger logger = Logger.getLogger(AsyncJobsService.class);

    // Shared by all job kinds of the extension: two jobs of one request may belong to different registries.
    private static final SessionLeases SESSION_LEASES = new SessionLeases();

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
     * @throws RejectedExecutionException if the executor of the registry has no room for another job; a
     *                                    {@link JobsRegistryShutDownException} if the registry is shut down. A job
     *                                    that is not started ends no session: a caller that asked {@link LogoutFilter}
     *                                    to keep the session must give it back, see
     *                                    {@link RequestContextUtil#releaseSession()}
     */
    public @NotNull String startJob(@Nullable P payload, int timeoutInMinutes, @NotNull JobTask<R> task) {
        if (timeoutInMinutes <= 0) {
            throw new IllegalArgumentException("Job timeout must be positive: " + timeoutInMinutes);
        }
        Subject userSubject = securityService.getCurrentSubject();
        boolean endsSession = userSubject != null && isJobLogoutRequired();
        if (endsSession) {
            // taken before the job can run: a job of the same request which finishes meanwhile must not end the session
            SESSION_LEASES.acquire(userSubject);
        }
        AsyncJob<P, R> job = new AsyncJob<>(UUID.randomUUID().toString(), securityService.getCurrentUser(), payload,
                () -> endSession(userSubject, endsSession));

        ScheduledFuture<?> deadline;
        boolean submitted = false;
        try {
            deadline = registry.submit(job, () -> runJob(job, task, userSubject),
                    () -> stop(job, StopRequest.timeout(timeoutInMinutes)), timeoutInMinutes);
            submitted = true;
        } catch (RejectedExecutionException e) {
            logger.warn("%s job is refused: %s".formatted(registry.getJobName(), e.getMessage()));
            throw e;
        } finally {
            if (!submitted && endsSession) {
                // the job never runs, whatever stopped it: its lease must not stay behind
                dropLeaseOfJobWhichNeverRan(userSubject);
            }
        }
        job.completion().whenComplete((result, thrown) -> deadline.cancel(false));
        // from here on the job ends the session kept for it, whatever else fails in this request
        RequestContextUtil.markJobStarted();
        return job.jobId();
    }

    /**
     * Cancels a job. Does nothing if the job is already over.
     * <p>
     * A job which still waits for a thread is cancelled at once and never runs. A running one is, with
     * {@link TimeoutPolicy#INTERRUPT}, cancelled at once and its worker thread interrupted; with
     * {@link TimeoutPolicy#COOPERATIVE}, asked to stop and still running until it stops - if it still finishes with a
     * result, that result counts.
     */
    public void cancelJob(@NotNull String jobId) {
        stop(getJob(jobId), StopRequest.cancel());
    }

    public @NotNull JobState getJobState(@NotNull String jobId) {
        return getJob(jobId).toJobState();
    }

    /**
     * @return the job result, or empty while the job is still running
     * @throws IllegalStateException if the job failed or was cancelled
     */
    public @NotNull Optional<R> getJobResult(@NotNull String jobId) {
        JobOutcome<R> outcome = getJob(jobId).getOutcome();
        if (outcome == null) {
            return Optional.empty();
        }
        if (outcome.status() != JobStatus.SUCCESSFULLY_FINISHED) {
            throw new IllegalStateException("%s job was cancelled or failed: %s".formatted(registry.getJobName(), outcome.errorMessage()));
        }
        return Optional.of(Objects.requireNonNull(outcome.result()));
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

    /**
     * Stops a job which is not over: for its deadline, or for a cancel.
     * <p>
     * A job which still waits for a thread has done nothing yet, so it ends whatever the policy, and never runs: here,
     * if it can be taken back from the queue, or else when it gets its thread.
     * A running one is ended here and interrupted with {@link TimeoutPolicy#INTERRUPT}, and only asked to stop with
     * {@link TimeoutPolicy#COOPERATIVE}: it then ends on its own thread.
     * <p>
     * Whoever ends the job, it ends with the first stop request it received: a deadline and a cancel which reach a job
     * together report the one which came first, the same one the job showed while it was stopping.
     */
    private void stop(@NotNull AsyncJob<P, R> job, @NotNull StopRequest request) {
        if (job.isOver()) {
            return;
        }
        StopRequest firstRequest = job.requestStop(request);
        if (registry.takeBackIfQueued(job)) {
            if (job.finish(firstRequest.toOutcome())) {
                logger.warn("%s job '%s' is stopped before it started: %s".formatted(registry.getJobName(), job.jobId(), firstRequest.message()));
            }
            job.releaseSession();
        } else if (registry.getTimeoutPolicy() == TimeoutPolicy.INTERRUPT) {
            if (job.finish(firstRequest.toOutcome())) {
                logger.warn("%s job '%s' is stopped: %s".formatted(registry.getJobName(), job.jobId(), firstRequest.message()));
                job.interruptWorker();
            }
        } else {
            logger.warn("%s job '%s' is asked to stop: %s".formatted(registry.getJobName(), job.jobId(), firstRequest.message()));
        }
    }

    @SuppressWarnings("java:S1181") // any throwable must end the job, or it stays "in progress" forever
    private void runJob(@NotNull AsyncJob<P, R> job, @NotNull JobTask<R> task, @Nullable Subject userSubject) {
        if (!job.claim()) {
            // the job was taken back while it waited for this thread, and has already ended
            return;
        }
        // attached before the check below, so a stop that comes after the check interrupts this thread
        job.attachWorker(Thread.currentThread());
        boolean ran = false;
        R result = null;
        Throwable failure = null;
        try {
            // A job can be stopped while it waits for a thread. With TimeoutPolicy.INTERRUPT the stop has ended it
            // already. With TimeoutPolicy.COOPERATIVE the stop only asked it to stop, since a running job stops at a
            // safe point - but this one has not started, so it has done nothing and ends here, as a job taken back
            // from the queue would. A stop which comes after this check reaches the task through isAbortRequested().
            StopRequest earlyStop = job.getStopRequest();
            if (earlyStop != null && job.finish(earlyStop.toOutcome())) {
                logger.warn("%s job '%s' is stopped before it started: %s".formatted(registry.getJobName(), job.jobId(), earlyStop.message()));
            }
            if (!job.isOver()) {
                ran = true;
                result = runAsUser(userSubject, () -> task.run(job));
            }
        } catch (Throwable e) {
            failure = e;
        } finally {
            job.detachWorker();
            // Nothing interrupts this thread for this job any more. An interrupt which a stop sent, and which the task
            // passed on by restoring the flag, must not reach the completion and the logout below.
            Thread.interrupted();
        }

        try {
            if (!ran) {
                logger.debug("%s job '%s' was over before it got a thread, so it did not run".formatted(registry.getJobName(), job.jobId()));
            } else if (failure == null) {
                finishWithResult(job, result);
            } else {
                finishWithFailure(job, failure);
            }
        } finally {
            // After the job is over, not before: a slow logout must not keep finished work "in progress", and an error
            // escaping it must not keep the job from ending. The session was kept alive for this job, whether it ran
            // or not.
            job.releaseSession();
        }
    }

    private void finishWithResult(@NotNull AsyncJob<P, R> job, @Nullable R result) {
        if (result == null) {
            if (job.finish(JobOutcome.failed(JobMessages.NO_RESULT))) {
                logger.error("%s job '%s' failed: %s".formatted(registry.getJobName(), job.jobId(), JobMessages.NO_RESULT));
            }
        } else if (job.finish(JobOutcome.succeeded(result))) {
            logger.debug("%s job '%s' is finished".formatted(registry.getJobName(), job.jobId()));
        } else {
            logger.debug("%s job '%s' finished after it was stopped, its result is dropped".formatted(registry.getJobName(), job.jobId()));
        }
    }

    /**
     * A job which was asked to stop and then failed is taken to have stopped as asked, and reports the reason of that
     * request. Any other failure is the job's own.
     */
    private void finishWithFailure(@NotNull AsyncJob<P, R> job, @NotNull Throwable failure) {
        StopRequest stop = job.getStopRequest();
        if (stop != null) {
            if (job.finish(stop.toOutcome())) {
                logger.warn("%s job '%s' stopped: %s".formatted(registry.getJobName(), job.jobId(), stop.message()));
            }
            return;
        }
        String reason = describeFailureSafely(failure);
        if (job.finish(JobOutcome.failed(reason))) {
            logger.error("%s job '%s' failed with error: %s".formatted(registry.getJobName(), job.jobId(), reason), failure);
        } else {
            logger.warn("%s job '%s' failed after it was over: %s".formatted(registry.getJobName(), job.jobId(), reason));
        }
    }

    /**
     * {@link #describeFailure(Throwable)} can be overridden: a description which fails itself, in any way, must not
     * keep the job from ending.
     */
    @SuppressWarnings("java:S1181") // any throwable must end the job, or it stays "in progress" forever
    private @NotNull String describeFailureSafely(@NotNull Throwable failure) {
        try {
            return describeFailure(failure);
        } catch (Throwable e) {
            logger.error("Cannot describe the failure of a %s job".formatted(registry.getJobName()), e);
            return failure.getClass().getName();
        }
    }

    private @Nullable R runAsUser(@Nullable Subject userSubject, @NotNull PrivilegedAction<R> action) {
        // a job started by a call without a subject runs as it is, the way it would have run on the request thread
        if (userSubject == null) {
            return action.run();
        }
        return securityService.doAsUser(userSubject, action);
    }

    /**
     * Ends the session kept for a job which is over, unless another job of the same session still runs: the last of
     * them ends it.
     */
    private void endSession(@Nullable Subject userSubject, boolean endsSession) {
        if (userSubject != null && endsSession && SESSION_LEASES.release(userSubject)) {
            logout(userSubject);
        }
    }

    /**
     * Gives back the lease of a job which was refused, or failed to start. The session is normally the caller's to give
     * back then, not this job's - unless an earlier job of the same request started, owns the session, and finished
     * while this job held its lease: that job left the session to the last lease, and this is it.
     */
    private void dropLeaseOfJobWhichNeverRan(@NotNull Subject userSubject) {
        if (SESSION_LEASES.release(userSubject) && RequestContextUtil.isJobStarted()) {
            logout(userSubject);
        }
    }

    private void logout(@NotNull Subject userSubject) {
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
