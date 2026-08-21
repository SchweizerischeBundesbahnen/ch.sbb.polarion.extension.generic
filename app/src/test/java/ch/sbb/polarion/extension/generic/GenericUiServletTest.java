package ch.sbb.polarion.extension.generic;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.Serial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

        // the generic/ prefix is no longer a route: it is served from the extension's own webapp
        TestServlet servlet = callServlet("/polarion/testServletName/ui/generic/genericUri/someImage.gif");
        verify(servlet, times(1)).serveResource(any(), eq("/generic/genericUri/someImage.gif"));

        // regular resource
        servlet = callServlet("/polarion/testServletName/ui/regularUri/someStyle.css");
        verify(servlet, times(1)).serveResource(any(), eq("/regularUri/someStyle.css"));
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

    @SneakyThrows
    private TestServlet callServlet(String uri) {
        TestServlet spy = spy(new TestServlet("testServletName"));
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getRequestURI()).thenReturn(uri);
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
