package ch.sbb.polarion.extension.generic.jobs;

import ch.sbb.polarion.extension.generic.rest.filter.LogoutFilter;
import ch.sbb.polarion.extension.generic.rest.model.jobs.JobStatus;
import com.polarion.platform.security.ISecurityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.security.auth.Subject;
import java.security.PrivilegedAction;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncJobsServiceTest {

    private static final String TEST_USER = "testUser";
    private static final String PAYLOAD = "payload";

    @Mock
    private ISecurityService securityService;

    @Mock
    private Subject subject;

    @Mock
    private ServletRequestAttributes requestAttributes;

    private JobsRegistry<String, String> interruptRegistry;
    private JobsRegistry<String, String> cooperativeRegistry;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        interruptRegistry = JobsRegistry.<String, String>builder("Test")
                .timeoutPolicy(TimeoutPolicy.INTERRUPT)
                .timeoutUnit(TimeUnit.MILLISECONDS)
                .build();
        cooperativeRegistry = JobsRegistry.<String, String>builder("Test")
                .timeoutPolicy(TimeoutPolicy.COOPERATIVE)
                .timeoutUnit(TimeUnit.MILLISECONDS)
                .build();
        RequestContextHolder.setRequestAttributes(requestAttributes);
        lenient().when(securityService.getCurrentUser()).thenReturn(TEST_USER);
        lenient().when(securityService.getCurrentSubject()).thenReturn(subject);
        lenient().when(securityService.doAsUser(eq(subject), any(PrivilegedAction.class)))
                .thenAnswer(invocation -> ((PrivilegedAction<?>) invocation.getArgument(1)).run());
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        interruptRegistry.shutdown();
        cooperativeRegistry.shutdown();
    }

    @Test
    void shouldRunJobAsUserAndReturnResult() throws Exception {
        asyncRequest();
        AsyncJobsService<String, String> service = service(interruptRegistry);

        String jobId = service.startJob(PAYLOAD, 60, control -> "result of " + control.jobId());

        awaitDone(interruptRegistry, jobId);
        JobState jobState = service.getJobState(jobId);
        assertThat(jobState.status()).isEqualTo(JobStatus.SUCCESSFULLY_FINISHED);
        assertThat(jobState.errorMessage()).isNull();
        assertThat(service.getJobResult(jobId)).contains("result of " + jobId);
        assertThat(service.getJobPayload(jobId)).isEqualTo(PAYLOAD);
        assertThat(service.getAllJobsStates()).containsOnlyKeys(jobId);
        verify(securityService).doAsUser(eq(subject), any(PrivilegedAction.class));
        verify(securityService, timeout(1000)).logout(subject);
    }

    @Test
    void shouldHideJobFromOtherUsers() throws Exception {
        AsyncJobsService<String, String> service = service(interruptRegistry);
        String jobId = service.startJob(PAYLOAD, 60, control -> "result");
        awaitDone(interruptRegistry, jobId);

        when(securityService.getCurrentUser()).thenReturn("other_" + TEST_USER);

        assertThatThrownBy(() -> service.getJobState(jobId)).isInstanceOf(NoSuchElementException.class).hasMessage("Test job is unknown: " + jobId);
        assertThatThrownBy(() -> service.getJobResult(jobId)).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> service.getJobPayload(jobId)).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> service.cancelJob(jobId)).isInstanceOf(NoSuchElementException.class);
        assertThat(service.getAllJobsStates()).isEmpty();
        assertThatThrownBy(() -> service.getJobState("unknown")).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void shouldHideJobWithoutUserFromEverybody() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(null);
        AsyncJobsService<String, String> service = service(interruptRegistry);
        String jobId = service.startJob(PAYLOAD, 60, control -> "result");
        awaitDone(interruptRegistry, jobId);

        assertThatThrownBy(() -> service.getJobState(jobId)).isInstanceOf(NoSuchElementException.class);
        assertThat(service.getAllJobsStates()).isEmpty();
    }

    @Test
    void shouldNotLogoutWhenSessionIsNotKeptForJob() throws Exception {
        when(requestAttributes.getAttribute(LogoutFilter.XSRF_SKIP_LOGOUT, RequestAttributes.SCOPE_REQUEST)).thenReturn(Boolean.TRUE);
        AsyncJobsService<String, String> service = service(interruptRegistry);

        String jobId = service.startJob(PAYLOAD, 60, control -> "result");

        awaitDone(interruptRegistry, jobId);
        verify(securityService, never()).logout(any());
    }

    @Test
    void shouldNotLogoutWithoutRequest() throws Exception {
        RequestContextHolder.resetRequestAttributes();
        AsyncJobsService<String, String> service = service(interruptRegistry);

        String jobId = service.startJob(PAYLOAD, 60, control -> "result");

        awaitDone(interruptRegistry, jobId);
        verify(securityService, never()).logout(any());
    }

    @Test
    void shouldRunJobDirectlyWithoutSubject() throws Exception {
        when(securityService.getCurrentSubject()).thenReturn(null);
        AsyncJobsService<String, String> service = service(interruptRegistry);

        String jobId = service.startJob(PAYLOAD, 60, control -> "result");

        awaitDone(interruptRegistry, jobId);
        assertThat(service.getJobResult(jobId)).contains("result");
        verify(securityService, never()).doAsUser(any(), any(PrivilegedAction.class));
    }

    @Test
    void shouldReportFailure() throws Exception {
        asyncRequest();
        AsyncJobsService<String, String> service = service(interruptRegistry);

        String jobId = service.startJob(PAYLOAD, 60, control -> {
            throw new IllegalStateException("test error");
        });

        awaitDone(interruptRegistry, jobId);
        JobState jobState = service.getJobState(jobId);
        assertThat(jobState.status()).isEqualTo(JobStatus.FAILED);
        assertThat(jobState.errorMessage()).isEqualTo("test error");
        assertThatThrownBy(() -> service.getJobResult(jobId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Test job was cancelled or failed: test error");
        verify(securityService, timeout(1000)).logout(subject);
    }

    @Test
    void shouldReportClassOfFailureWithoutMessage() throws Exception {
        AsyncJobsService<String, String> service = service(interruptRegistry);

        String jobId = service.startJob(PAYLOAD, 60, control -> {
            throw new ConcurrentModificationException();
        });

        awaitDone(interruptRegistry, jobId);
        assertThat(service.getJobState(jobId).errorMessage()).isEqualTo(ConcurrentModificationException.class.getName());
    }

    @Test
    void shouldReportErrors() throws Exception {
        AsyncJobsService<String, String> service = service(interruptRegistry);

        String jobId = service.startJob(PAYLOAD, 60, control -> {
            throw new AssertionError("broken");
        });

        awaitDone(interruptRegistry, jobId);
        assertThat(service.getJobState(jobId).status()).isEqualTo(JobStatus.FAILED);
        assertThat(service.getJobState(jobId).errorMessage()).isEqualTo("broken");
    }

    @Test
    void shouldReturnEmptyResultWhileRunning() {
        CountDownLatch release = new CountDownLatch(1);
        AsyncJobsService<String, String> service = service(interruptRegistry);

        String jobId = service.startJob(PAYLOAD, 60_000, control -> {
            awaitQuietly(release);
            return "result";
        });

        assertThat(service.getJobResult(jobId)).isEmpty();
        assertThat(service.getJobState(jobId).status()).isEqualTo(JobStatus.IN_PROGRESS);
        release.countDown();
    }

    @Test
    void shouldReportProgress() throws Exception {
        CountDownLatch reported = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AsyncJobsService<String, String> service = service(cooperativeRegistry);

        String jobId = service.startJob(PAYLOAD, 60_000, control -> {
            control.reportProgress("step 1");
            reported.countDown();
            awaitQuietly(release);
            return "result";
        });

        assertThat(reported.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(service.getJobState(jobId).progressMessage()).isEqualTo("step 1");
        release.countDown();
        awaitDone(cooperativeRegistry, jobId);
    }

    @Test
    void shouldInterruptJobOnTimeout() throws Exception {
        AtomicBoolean interrupted = new AtomicBoolean();
        CountDownLatch stopped = new CountDownLatch(1);
        AsyncJobsService<String, String> service = service(interruptRegistry);

        String jobId = service.startJob(PAYLOAD, 50, control -> {
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                interrupted.set(true);
                Thread.currentThread().interrupt();
            } finally {
                stopped.countDown();
            }
            throw new IllegalStateException("interrupted");
        });

        awaitDone(interruptRegistry, jobId);
        JobState jobState = service.getJobState(jobId);
        assertThat(jobState.status()).isEqualTo(JobStatus.FAILED);
        assertThat(jobState.errorMessage()).isEqualTo("Timeout after 50 min");
        assertThat(stopped.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(interrupted).isTrue();
        // the failure the interrupt caused does not replace the timeout
        assertThat(service.getJobState(jobId).errorMessage()).isEqualTo("Timeout after 50 min");
    }

    @Test
    void shouldCancelAndInterruptJob() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch stopped = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();
        AsyncJobsService<String, String> service = service(interruptRegistry);

        String jobId = service.startJob(PAYLOAD, 60_000, control -> {
            started.countDown();
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                interrupted.set(true);
                Thread.currentThread().interrupt();
            } finally {
                stopped.countDown();
            }
            throw new IllegalStateException("interrupted");
        });
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();

        service.cancelJob(jobId);

        JobState jobState = service.getJobState(jobId);
        assertThat(jobState.status()).isEqualTo(JobStatus.CANCELLED);
        assertThat(jobState.errorMessage()).isEqualTo(AsyncJobsService.CANCELLED_BY_USER_MESSAGE);
        assertThat(stopped.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(interrupted).isTrue();
        assertThat(service.getJobState(jobId).errorMessage()).isEqualTo(AsyncJobsService.CANCELLED_BY_USER_MESSAGE);
        assertThatThrownBy(() -> service.getJobResult(jobId)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldIgnoreCancelOfFinishedJob() throws Exception {
        AsyncJobsService<String, String> service = service(interruptRegistry);
        String jobId = service.startJob(PAYLOAD, 60, control -> "result");
        awaitDone(interruptRegistry, jobId);

        service.cancelJob(jobId);

        assertThat(service.getJobState(jobId).status()).isEqualTo(JobStatus.SUCCESSFULLY_FINISHED);
        assertThat(service.getJobState(jobId).errorMessage()).isNull();
    }

    @Test
    void shouldAskCooperativeJobToStopOnTimeout() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        AsyncJobsService<String, String> service = service(cooperativeRegistry);

        String jobId = service.startJob(PAYLOAD, 50, control -> {
            while (!control.isAbortRequested()) {
                Thread.onSpinWait();
            }
            // still running after the deadline, until it stops by itself
            awaitQuietly(release);
            // a task that stops on its own may say so with the exception made for it
            throw new CancellationException("aborted");
        });

        AsyncJob<String, String> job = cooperativeRegistry.getJob(jobId);
        assertThat(job).isNotNull();
        long deadline = System.currentTimeMillis() + 5000;
        while (!job.isAbortRequested() && System.currentTimeMillis() < deadline) {
            Thread.onSpinWait();
        }
        assertThat(service.getJobState(jobId).status()).isEqualTo(JobStatus.IN_PROGRESS);

        release.countDown();
        awaitDone(cooperativeRegistry, jobId);
        JobState jobState = service.getJobState(jobId);
        assertThat(jobState.status()).isEqualTo(JobStatus.FAILED);
        assertThat(jobState.errorMessage()).isEqualTo("Timeout after 50 min");
    }

    @Test
    void shouldCancelCooperativeJob() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        AsyncJobsService<String, String> service = service(cooperativeRegistry);

        String jobId = service.startJob(PAYLOAD, 60_000, control -> {
            started.countDown();
            while (!control.isAbortRequested()) {
                Thread.onSpinWait();
            }
            throw new IllegalStateException("aborted");
        });
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();

        service.cancelJob(jobId);

        awaitDone(cooperativeRegistry, jobId);
        JobState jobState = service.getJobState(jobId);
        assertThat(jobState.status()).isEqualTo(JobStatus.CANCELLED);
        assertThat(jobState.errorMessage()).isEqualTo(AsyncJobsService.CANCELLED_BY_USER_MESSAGE);
    }

    @Test
    void shouldKeepResultOfCooperativeJobFinishedAfterCancel() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        AsyncJobsService<String, String> service = service(cooperativeRegistry);

        String jobId = service.startJob(PAYLOAD, 60_000, control -> {
            started.countDown();
            while (!control.isAbortRequested()) {
                Thread.onSpinWait();
            }
            return "partial result";
        });
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();

        service.cancelJob(jobId);

        awaitDone(cooperativeRegistry, jobId);
        JobState jobState = service.getJobState(jobId);
        assertThat(jobState.status()).isEqualTo(JobStatus.SUCCESSFULLY_FINISHED);
        assertThat(jobState.errorMessage()).isNull();
        assertThat(service.getJobResult(jobId)).contains("partial result");
    }

    @Test
    void shouldRefuseJobBeyondExecutorBounds() {
        asyncRequest();
        CountDownLatch release = new CountDownLatch(1);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new SynchronousQueue<>());
        JobsRegistry<String, String> boundedRegistry = JobsRegistry.<String, String>builder("Bounded").executor(executor).build();
        try {
            AsyncJobsService<String, String> service = service(boundedRegistry);
            String runningJobId = service.startJob(PAYLOAD, 60, control -> {
                awaitQuietly(release);
                return "result";
            });

            assertThatThrownBy(() -> service.startJob(PAYLOAD, 60, control -> "result"))
                    .isInstanceOf(RejectedExecutionException.class);
            assertThat(service.getAllJobsStates()).containsOnlyKeys(runningJobId);
            // the refused job ends no session: the caller gives it back
            verify(securityService, never()).logout(any());
        } finally {
            release.countDown();
            boundedRegistry.shutdown();
        }
    }

    @Test
    void shouldRefuseNonPositiveTimeout() {
        AsyncJobsService<String, String> service = service(interruptRegistry);

        assertThatThrownBy(() -> service.startJob(PAYLOAD, 0, control -> "result")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldUnwrapFutureWrappers() {
        IllegalStateException cause = new IllegalStateException("cause");

        assertThat(AsyncJobsService.rootReason(new CompletionException(new ExecutionException(cause)))).isSameAs(cause);
        assertThat(AsyncJobsService.rootReason(new CompletionException(null))).isInstanceOf(CompletionException.class);
        assertThat(service(interruptRegistry).describeFailure(new CompletionException(cause))).isEqualTo("cause");
    }

    private AsyncJobsService<String, String> service(JobsRegistry<String, String> registry) {
        return new AsyncJobsService<>(registry, securityService);
    }

    private void asyncRequest() {
        when(requestAttributes.getAttribute(LogoutFilter.XSRF_SKIP_LOGOUT, RequestAttributes.SCOPE_REQUEST)).thenReturn(Boolean.FALSE);
        when(requestAttributes.getAttribute(LogoutFilter.ASYNC_SKIP_LOGOUT, RequestAttributes.SCOPE_REQUEST)).thenReturn(Boolean.TRUE);
    }

    static void awaitDone(JobsRegistry<?, ?> registry, String jobId) throws Exception {
        AsyncJob<?, ?> job = registry.getJob(jobId);
        assertThat(job).isNotNull();
        CompletableFuture<?> future = job.getFuture();
        future.handle((result, thrown) -> null).get(5, TimeUnit.SECONDS);
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
