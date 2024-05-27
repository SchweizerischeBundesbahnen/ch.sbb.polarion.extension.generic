package ch.sbb.polarion.extension.generic.util;

import ch.sbb.polarion.extension.generic.rest.filter.AuthenticationFilter;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;
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
}
