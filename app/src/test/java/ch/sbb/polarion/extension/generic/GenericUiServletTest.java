package ch.sbb.polarion.extension.generic;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenericUiServletTest {

    @Test
    void testSetContentType() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        GenericUiServlet.setContentType("app.js", response);
        verify(response, times(1)).setContentType("text/javascript");

        response = mock(HttpServletResponse.class);
        GenericUiServlet.setContentType("/sub_path/file.html", response);
        verify(response, times(1)).setContentType("text/html");

        response = mock(HttpServletResponse.class);
        GenericUiServlet.setContentType("https://localhost/styles.css", response);
        verify(response, times(1)).setContentType("text/css");

        response = mock(HttpServletResponse.class);
        GenericUiServlet.setContentType("/img.png", response);
        verify(response, times(1)).setContentType("image/png");

        response = mock(HttpServletResponse.class);
        GenericUiServlet.setContentType("/img.svg", response);
        verify(response, times(1)).setContentType("image/svg+xml");

        response = mock(HttpServletResponse.class);
        GenericUiServlet.setContentType("/img.gif", response);
        verify(response, times(1)).setContentType("image/gif");

        HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> GenericUiServlet.setContentType("unknown_file.xml", servletResponse));
        assertEquals("Unsupported file type", exception.getMessage());
    }

    @Test
    @SneakyThrows
    void testService() {
        // error case (at least we assume that uri will start with /polarion/)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> callServlet("/badUrl/someImg.png"));
        assertEquals("Unsupported resource path", exception.getMessage());

        // we expect uri starting by /polarion/{web_app_name}/ui/
        exception = assertThrows(IllegalArgumentException.class, () -> callServlet("/polarion/testServletName/unknownPath"));
        assertEquals("Unsupported resource path", exception.getMessage());

        // generic resource
        TestServlet servlet = callServlet("/polarion/testServletName/ui/generic/genericUri/someImage.gif");
        verify(servlet, times(1)).serveGenericResource(any(), eq("genericUri/someImage.gif"));
        verify(servlet, times(0)).serveResource(any(), any());

        // regular resource
        servlet = callServlet("/polarion/testServletName/ui/regularUri/someStyle.css");
        verify(servlet, times(0)).serveGenericResource(any(), any());
        verify(servlet, times(1)).serveResource(any(), any());
    }

    @SneakyThrows
    private TestServlet callServlet(String uri) {
        TestServlet spy = spy(new TestServlet("testServletName"));
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getRequestURI()).thenReturn(uri);
        lenient().doNothing().when(spy).serveGenericResource(any(), any());
        lenient().doNothing().when(spy).serveResource(any(), any());
        spy.service(request, response);
        return spy;
    }

    public static class TestServlet extends GenericUiServlet {

        @Serial
        private static final long serialVersionUID = 7300367869059799910L;

        protected TestServlet(String webAppName) {
            super(webAppName);
        }
    }
}
