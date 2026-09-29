package ch.sbb.polarion.extension.generic.util;

import ch.sbb.polarion.extension.generic.rest.filter.AuthenticationFilter;
import ch.sbb.polarion.extension.generic.rest.filter.LogoutFilter;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.security.auth.Subject;

@UtilityClass
public final class RequestContextUtil {

    @Nullable
    public static Subject getUserSubject() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            return (Subject) requestAttributes.getRequest().getAttribute(AuthenticationFilter.USER_SUBJECT);
        } else {
            throw new IllegalStateException("Cannot find request attributes in the request context");
        }
    }

    /**
     * Keeps the session of the current user past the response of this request, for an asynchronous job the request
     * only starts: {@link LogoutFilter} would end that session as soon as the response is written, long before the job
     * is over. The job ends that session itself when it is over.
     * <p>
     * Call it before the job is started, since that is where it is read, and call {@link #releaseSession()} if the
     * request fails before the job has started: nothing else would end the session then.
     */
    public static void keepSessionAlive() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            requestAttributes.setAttribute(LogoutFilter.ASYNC_SKIP_LOGOUT, Boolean.TRUE, RequestAttributes.SCOPE_REQUEST);
        }
    }

    /**
     * Gives the session of the current user back to {@link LogoutFilter}, for a request which asked to keep it with
     * {@link #keepSessionAlive()} and then did not start the job which was to end it.
     */
    public static void releaseSession() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            requestAttributes.removeAttribute(LogoutFilter.ASYNC_SKIP_LOGOUT, RequestAttributes.SCOPE_REQUEST);
        }
    }
}
