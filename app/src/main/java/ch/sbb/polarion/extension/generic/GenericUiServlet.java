package ch.sbb.polarion.extension.generic;

import com.polarion.alm.shared.util.Pair;
import com.polarion.core.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    /**
     * Sentinel root used only to validate, via {@link Path#normalize()} +
     * {@link Path#startsWith(Path)}, that a relative resource path stays inside it.
     * This containment check is the form CodeQL recognizes as a path-injection
     * barrier ({@code java/path-injection}); see {@link #sanitizeResourcePath(String)}.
     */
    private static final Path RESOURCE_ROOT = Paths.get("/__ui_resource_root__");

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
        return sanitizeResourcePath(relative);
    }

    /**
     * Validates and normalizes a relative UI resource path, returning the cleaned,
     * {@code /}-separated path, or throwing {@link IllegalArgumentException} on any
     * path-traversal attempt.
     * <p>
     * Steps:
     * <ol>
     *   <li>Percent-encoded separators ({@code %2F}/{@code %5C}, any case) are
     *       decoded first, so an encoded payload is treated exactly as a downstream
     *       decoder would unescape it (defense against a container that decodes
     *       after this check).</li>
     *   <li>A backslash anywhere is rejected: it is not a valid URL path separator
     *       and is a common Windows-style traversal bypass. Doing this explicitly
     *       keeps the behaviour identical on every host OS (on Linux {@code \} is a
     *       legal filename char and would otherwise slip through).</li>
     *   <li>The path is resolved against a sentinel root and normalized; if
     *       normalization escapes the root (e.g. via {@code ..} or an absolute
     *       path) it is rejected. This {@link Path#normalize()} + {@link
     *       Path#startsWith(Path)} containment is also the barrier CodeQL
     *       recognizes for {@code java/path-injection}.</li>
     * </ol>
     * {@code ..} inside a filename (e.g. Turbopack chunk names like
     * {@code chunk..hash.js}) is NOT traversal and is preserved.
     */
    @VisibleForTesting
    static @NotNull String sanitizeResourcePath(@NotNull String relative) {
        String decoded = relative
                .replace("%2f", "/").replace("%2F", "/")
                .replace("%5c", "\\").replace("%5C", "\\");
        if (decoded.indexOf('\\') >= 0) {
            throw new IllegalArgumentException("Path traversal not allowed");
        }
        Path resolved = RESOURCE_ROOT.resolve(decoded).normalize();
        if (!resolved.startsWith(RESOURCE_ROOT)) {
            throw new IllegalArgumentException("Path traversal not allowed");
        }
        return RESOURCE_ROOT.relativize(resolved).toString().replace('\\', '/');
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
