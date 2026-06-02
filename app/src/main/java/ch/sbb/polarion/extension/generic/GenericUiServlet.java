package ch.sbb.polarion.extension.generic;

import com.polarion.alm.shared.util.Pair;
import com.polarion.core.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public abstract class GenericUiServlet extends HttpServlet {

    private static final List<Pair<String, String>> ALLOWED_FILE_TYPES = Arrays.asList(
            Pair.of(".js", "text/javascript"),
            Pair.of(".html", "text/html"),
            Pair.of(".css", "text/css"),
            Pair.of(".png", "image/png"),
            Pair.of(".svg", "image/svg+xml"),
            Pair.of(".gif", "image/gif"),
            Pair.of(".woff", "application/font-woff"),
            Pair.of(".woff2", "application/font-woff2"),
            Pair.of(".ico", "image/x-icon"),
            Pair.of(".txt", "text/plain")
    );

    private static final Logger logger = Logger.getLogger(GenericUiServlet.class);

    @Serial
    private static final long serialVersionUID = 4323903250755251706L;

    protected final String webAppName;

    protected GenericUiServlet(String webAppName) {
        this.webAppName = webAppName;
    }

    @VisibleForTesting
    static void setContentType(@NotNull String uri, @NotNull HttpServletResponse response) {
        response.setContentType(ALLOWED_FILE_TYPES.stream().filter(f -> uri.endsWith(f.left())).map(Pair::right).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported file type")));
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String resourceUri = getInternalResourcePath(request.getRequestURI());
        try {
            if (resourceUri.startsWith("generic/")) {
                serveGenericResource(response, resourceUri.substring("generic/".length()));
            } else {
                serveResource(response, "/" + resourceUri);
            }
        } catch (IOException e) {
            logger.error("Cannot copy resource '" + resourceUri + "': " + e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error by getting resource '" + resourceUri + "': " + e.getMessage(), e);
            throw new ServletException(e.getMessage(), e);
        }
    }

    private String getInternalResourcePath(String fullUri) {
        String acceptablePath = "/polarion/" + webAppName + "/ui/";
        if (!fullUri.startsWith(acceptablePath)) {
            throw new IllegalArgumentException("Unsupported resource path");
        }
        if (ALLOWED_FILE_TYPES.stream().noneMatch(t -> fullUri.endsWith(t.left()))) {
            throw new IllegalArgumentException("Unsupported file type");
        }
        String relative = fullUri.substring(acceptablePath.length());
        if (containsPathTraversal(relative)) {
            throw new IllegalArgumentException("Path traversal not allowed");
        }
        return relative;
    }

    /**
     * Rejects path-traversal attempts in a relative resource path while still
     * permitting filenames that merely contain {@code ..} (e.g. Turbopack chunk
     * names like {@code chunk..hash.js}).
     * <p>
     * Without this guard, a request like {@code /polarion/<app>/ui/../some.css}
     * would resolve through {@code ..} inside
     * {@link javax.servlet.ServletContext#getResourceAsStream(String)} and could
     * expose files outside the intended UI resource directory
     * (see CodeQL alert {@code java/path-injection}).
     * <p>
     * Returns {@code true} if any of the following holds:
     * <ul>
     *   <li>the path contains a backslash (not a valid URL separator and a common
     *       Windows-style traversal bypass);</li>
     *   <li>the path starts with {@code /} or contains an empty segment
     *       ({@code //}) — both can collapse the path or escape the configured root;</li>
     *   <li>any path segment (substring between {@code /} separators, or at the
     *       boundaries) is exactly {@code ".."}.</li>
     * </ul>
     * Note that {@code ..} inside a filename — such as {@code chunk..hash.js} or
     * {@code page/asset..v2.css} — is NOT treated as traversal.
     * <p>
     * Percent-encoded separators ({@code %2F}, {@code %5C}) are intentionally NOT
     * decoded here: the servlet container has already decoded the request URI by
     * the time this method runs, so any real separator is present as a literal
     * {@code /} or {@code \} and caught above. If a downstream layer ever bypasses
     * that decoding, {@link javax.servlet.ServletContext#getResourceAsStream(String)}
     * and {@link java.util.zip.ZipFile#getEntry(String)} treat the percent-encoded
     * forms as literal filename characters, not separators, so no traversal is
     * possible either way. Do NOT re-broaden this to a naive
     * {@code relative.contains("..")} — that would falsely reject legitimate
     * filenames containing {@code ..} (Turbopack chunks).
     */
    @VisibleForTesting
    static boolean containsPathTraversal(@NotNull String relative) {
        if (relative.indexOf('\\') >= 0) {
            return true;
        }
        if (relative.startsWith("/") || relative.contains("//")) {
            return true;
        }
        for (String segment : relative.split("/", -1)) {
            if ("..".equals(segment)) {
                return true;
            }
        }
        return false;
    }

    @VisibleForTesting
    void serveGenericResource(@NotNull HttpServletResponse response, @NotNull String uri) throws IOException {
        File resolvedJarFile = resolveJarFile(getCodeLocation());
        try (ZipFile zipFile = new ZipFile(resolvedJarFile)) {
            final ZipEntry zipEntry = zipFile.getEntry(uri);
            try (InputStream inputStream = zipFile.getInputStream(zipEntry)) {
                if (inputStream == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                } else {
                    try (ServletOutputStream outputStream = response.getOutputStream()) {
                        setContentType(uri, response);
                        IOUtils.copy(inputStream, outputStream);
                    }
                }
            }
        }
    }

    @VisibleForTesting
    @NotNull URL getCodeLocation() {
        return GenericUiServlet.class.getProtectionDomain().getCodeSource().getLocation();
    }

    @VisibleForTesting
    static @NotNull File resolveJarFile(@NotNull URL location) {
        try {
            return new File(location.toURI());
        } catch (URISyntaxException e) {
            // CodeSource.getLocation() may return a URL with unencoded characters
            // (e.g. literal spaces from a Windows username). URI.toURI() rejects
            // those, so fall back to decoding the raw path.
            return new File(URLDecoder.decode(location.getPath(), StandardCharsets.UTF_8));
        }
    }

    @VisibleForTesting
    void serveResource(@NotNull HttpServletResponse response, @NotNull String uri) throws IOException {
        try (InputStream inputStream = getServletContext().getResourceAsStream(uri)) {
            if (inputStream == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                try (ServletOutputStream outputStream = response.getOutputStream()) {
                    setContentType(uri, response);
                    IOUtils.copy(inputStream, outputStream);
                }
            }
        }
    }
}
