package ch.sbb.polarion.extension.generic;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
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

        // path under /ui/ but with a disallowed file extension
        exception = assertThrows(IllegalArgumentException.class, () -> callServlet("/polarion/testServletName/ui/evil.exe"));
        assertEquals("Unsupported file type", exception.getMessage());

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

        // leading slash after the prefix (URI like `/polarion/<app>/ui//bypass.css`
        // strips to `/bypass.css`, an absolute path that escapes the sentinel root)
        exception = assertThrows(IllegalArgumentException.class,
                () -> callServlet("/polarion/testServletName/ui//bypass.css"));
        assertEquals("Path traversal not allowed", exception.getMessage());

        // generic-prefixed traversal that escapes the root
        exception = assertThrows(IllegalArgumentException.class,
                () -> callServlet("/polarion/testServletName/ui/generic/../../escape.html"));
        assertEquals("Path traversal not allowed", exception.getMessage());
    }

    @Test
    @SneakyThrows
    void testServiceAllowsTurbopackChunkNamesWithDoubleDot() {
        // Turbopack/Next.js can emit chunk filenames that contain `..` inside the
        // filename itself (e.g. `chunk..hash.js`). These are NOT path traversal
        // and must be served normally.
        TestServlet servlet = callServlet("/polarion/testServletName/ui/_next/static/chunks/page..a1b2c3.js");
        verify(servlet, times(1)).serveResource(any(), eq("/_next/static/chunks/page..a1b2c3.js"));
        verify(servlet, times(0)).serveGenericResource(any(), any());

        servlet = callServlet("/polarion/testServletName/ui/asset..v2.css");
        verify(servlet, times(1)).serveResource(any(), eq("/asset..v2.css"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // ".." that escapes the root
            "..",
            "../foo.css",
            "../../foo.css",
            "foo/../../bar.css",
            "../sub/..//foo.css",
            // absolute path (leading slash) — resolve() makes it absolute, escaping the root
            "/foo.css",
            "/sub/foo.css",
            // backslash anywhere — not a valid URL separator, rejected on every OS
            "a\\b.css",
            "..\\foo.css",
            "foo\\..\\bar.css",
            "foo/sub\\evil.css",
            "\\evil.css",
            "foo.css\\",
            // Percent-encoded separators (%2F = '/', %5C = '\') decode first, then the
            // checks above run — so an encoded payload is treated as a downstream
            // decoder would unescape it.
            "..%2ffoo.css",
            "..%2Ffoo.css",
            "%2f..%2fevil.css",
            "..%5cfoo.css",
            "..%5Cfoo.css",
            "foo%5cbar.css"
    })
    void sanitizeResourcePath_rejectsTraversal(String path) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> GenericUiServlet.sanitizeResourcePath(path),
                "expected to reject traversal in: " + path);
        assertEquals("Path traversal not allowed", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // ordinary safe paths
            "foo.css",
            "app.js",
            "sub/foo.css",
            "deep/nested/path/foo.css",
            "_next/static/chunks/main.js",
            // ".." as part of the FILENAME (Turbopack-style) — must be allowed
            "chunk..hash.js",
            "page..a1b2c3.js",
            "asset..v2.css",
            "foo..bar.css",
            "a/foo..bar.css",
            "_next/static/chunks/page..a1b2c3.js",
            "_next/static/chunks/[id]..[hash].js",
            // ".." inside the middle of a segment, not as a full segment
            "foo..bar/baz.css",
            "foo/bar..baz.css",
            "a/b..c/d.css",
            "a/b../c.css",
            "a/..b/c.css",
            // names that start or end with ".." but are not the literal ".." segment
            "..foo.css",
            "foo...css",
            "...css",
            "....js",
            // single-dot segments collapse but stay inside the root
            "./foo.css",
            "a/./b.css",
            ".foo.css",
            // within-root ".." that does NOT escape — collapses to a path inside the root
            "foo/../bar.css",
            "foo/bar/../baz.css",
            "a/b/c/../../d.css",
            // empty segments collapse harmlessly
            "foo//bar.css",
            "a/b//c.css",
            // hashed/versioned filenames
            "main.abc123def..v2.js",
            "[locale]..page.js",
            // Percent-encoded separators decode to literal '/'. A standalone encoded
            // slash that is NOT next to ".." is just a subdirectory reference; an
            // encoded ".." that stays within the root decodes and collapses safely.
            "foo%2fbar.css",
            "foo%2Fbar.css",
            "sub%2fchunk..hash.js",
            "foo%2f..%2fbar.css"
    })
    void sanitizeResourcePath_allowsAndCleansSafePaths(String path) {
        String cleaned = GenericUiServlet.sanitizeResourcePath(path);
        // never escapes, never absolute, always '/'-separated
        assertFalse(cleaned.startsWith("/"), "must stay relative: " + cleaned);
        assertFalse(cleaned.contains("\\"), "must be '/'-separated: " + cleaned);
    }

    @Test
    void sanitizeResourcePathReturnsCleanedPath() {
        // ".." inside a filename is preserved
        assertEquals("chunk..hash.js", GenericUiServlet.sanitizeResourcePath("chunk..hash.js"));
        assertEquals("generic/genericUri/someImage.gif",
                GenericUiServlet.sanitizeResourcePath("generic/genericUri/someImage.gif"));
        // within-root ".." and empty segments collapse to a clean path
        assertEquals("bar.css", GenericUiServlet.sanitizeResourcePath("foo/../bar.css"));
        assertEquals("foo/bar.css", GenericUiServlet.sanitizeResourcePath("foo//bar.css"));
        // a within-root ".." under the generic/ prefix resolves to a regular resource
        assertEquals("escape.html", GenericUiServlet.sanitizeResourcePath("generic/../escape.html"));
        // encoded separators decode to subdirectories; an encoded within-root ".."
        // decodes and then collapses just like its literal form
        assertEquals("foo/bar.css", GenericUiServlet.sanitizeResourcePath("foo%2fbar.css"));
        assertEquals("bar.css", GenericUiServlet.sanitizeResourcePath("foo%2f..%2fbar.css"));
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
        // Compare File objects rather than the raw path string: File.getPath() canonicalizes
        // "/C:/..." differently per OS (Windows strips the leading slash before the drive letter,
        // Unix keeps it). Both sides go through the same normalization, so this holds on either.
        assertEquals(new File("/C:/Users/test folder with spaces/workspace/some-extension.jar"), resolved);
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
