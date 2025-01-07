package ch.sbb.polarion.extension.generic.util;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.security.auth.Subject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestContextUtilTest {

    @Test
    void shouldReturnUserSubject() {
        // Arrange
        ServletRequestAttributes requestAttributes = mock(ServletRequestAttributes.class, Answers.RETURNS_DEEP_STUBS);
        Subject subject = mock(Subject.class);
        when(requestAttributes
                .getRequest()
                .getAttribute("user_subject")).thenReturn(subject);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        // Act
        Subject resultSubject = RequestContextUtil.getUserSubject();

        // Assert
        assertThat(resultSubject).isEqualTo(subject);
    }

    @Test
    void shouldThrowIllegalStateExceptionByNullAttributes() {
        assertThatThrownBy(RequestContextUtil::getUserSubject)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("request attributes");
    }
}
