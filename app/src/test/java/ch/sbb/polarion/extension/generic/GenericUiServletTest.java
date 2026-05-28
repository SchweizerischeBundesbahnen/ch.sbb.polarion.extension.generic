package ch.sbb.polarion.extension.generic;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.Serial;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
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

        response = mock(HttpServletResponse.class);
        GenericUiServlet.setContentType("/somFont.woff", response);
        verify(response, times(1)).setContentType("application/font-woff");

        response = mock(HttpServletResponse.class);
        GenericUiServlet.setContentType("/somFont.woff2", response);
        verify(response, times(1)).setContentType("application/font-woff2");

        response = mock(HttpServletResponse.class);
        GenericUiServlet.setContentType("/img.ico", response);
        verify(response, times(1)).setContentType("image/x-icon");

        response = mock(HttpServletResponse.class);
        GenericUiServlet.setContentType("/data.txt", response);
        verify(response, times(1)).setContentType("text/plain");

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

    @Test
    @SneakyThrows
    void testServiceRejectsPathTraversal() {
        // `..` segment — would otherwise pass the prefix and suffix checks and
        // reach getServletContext().getResourceAsStream(...) (CodeQL alert
        // java/path-injection #5).
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> callServlet("/polarion/testServletName/ui/../some.css"));
        assertEquals("Path traversal not allowed", exception.getMessage());

        // backslash
        exception = assertThrows(IllegalArgumentException.class,
                () -> callServlet("/polarion/testServletName/ui/sub\\evil.css"));
        assertEquals("Path traversal not allowed", exception.getMessage());

        // double slash inside the path
        exception = assertThrows(IllegalArgumentException.class,
                () -> callServlet("/polarion/testServletName/ui/foo//bar.css"));
        assertEquals("Path traversal not allowed", exception.getMessage());

        // leading slash after the prefix (URI like `/polarion/<app>/ui//bypass.css`
        // strips to `/bypass.css` — caught by `startsWith("/")`)
        exception = assertThrows(IllegalArgumentException.class,
                () -> callServlet("/polarion/testServletName/ui//bypass.css"));
        assertEquals("Path traversal not allowed", exception.getMessage());

        // generic-prefixed traversal
        exception = assertThrows(IllegalArgumentException.class,
                () -> callServlet("/polarion/testServletName/ui/generic/../escape.html"));
        assertEquals("Path traversal not allowed", exception.getMessage());
    }

    @Test
    @SneakyThrows
    void testResolveJarFileWithRegularPath() {
        URL location = URI.create("file:/tmp/some-extension.jar").toURL();
        File resolved = GenericUiServlet.resolveJarFile(location);
        assertEquals(new File("/tmp/some-extension.jar"), resolved);
    }

    @Test
    @SneakyThrows
    void testResolveJarFileWithPercentEncodedSpace() {
        URL location = URI.create("file:/tmp/dir%20with%20space/some-extension.jar").toURL();
        File resolved = GenericUiServlet.resolveJarFile(location);
        assertEquals(new File("/tmp/dir with space/some-extension.jar"), resolved);
    }

    @Test
    @SneakyThrows
    @SuppressWarnings("deprecation") // URI.create rejects unencoded characters; the test
    // intentionally constructs a URL like the one CodeSource.getLocation() returns on
    // Windows when the username contains spaces — that is the bug being reproduced.
    void testResolveJarFileWithLiteralSpace() {
        URL location = new URL("file:/C:/Users/test folder with spaces/workspace/some-extension.jar");
        File resolved = GenericUiServlet.resolveJarFile(location);
        assertEquals("/C:/Users/test folder with spaces/workspace/some-extension.jar", resolved.getPath().replace('\\', '/'));
    }

    @Test
    @SneakyThrows
    void testServeGenericResourceServesEntryFromJar(@TempDir Path tempDir) {
        // Test the full serveGenericResource flow against a real ZIP — covers the
        // resolveJarFile(getCodeLocation()) wiring inside the try-with-resources.
        // Includes a literal space in the path to mirror the original failure mode.
        Path dirWithSpace = Files.createDirectory(tempDir.resolve("dir with space"));
        Path jarPath = dirWithSpace.resolve("fake-extension.jar");
        byte[] payload = "console.log('hi');".getBytes();
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(jarPath))) {
            zos.putNextEntry(new ZipEntry("genericUri/file.js"));
            zos.write(payload);
            zos.closeEntry();
        }

        TestServlet servlet = new TestServlet("testServletName", jarPath.toUri().toURL());
        HttpServletResponse response = mock(HttpServletResponse.class);
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(new CapturingServletOutputStream(captured));

        servlet.serveGenericResource(response, "genericUri/file.js");

        verify(response).setContentType("text/javascript");
        assertArrayEquals(payload, captured.toByteArray());
        verify(response, never()).sendError(anyInt());
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

        private final URL codeLocationOverride;

        protected TestServlet(String webAppName) {
            this(webAppName, null);
        }

        protected TestServlet(String webAppName, URL codeLocationOverride) {
            super(webAppName);
            this.codeLocationOverride = codeLocationOverride;
        }

        @Override
        URL getCodeLocation() {
            return codeLocationOverride != null ? codeLocationOverride : super.getCodeLocation();
        }
    }

    private static final class CapturingServletOutputStream extends ServletOutputStream {
        private final ByteArrayOutputStream sink;

        CapturingServletOutputStream(ByteArrayOutputStream sink) {
            this.sink = sink;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            // not needed for blocking writes in tests
        }

        @Override
        public void write(int b) {
            sink.write(b);
        }
    }
}
