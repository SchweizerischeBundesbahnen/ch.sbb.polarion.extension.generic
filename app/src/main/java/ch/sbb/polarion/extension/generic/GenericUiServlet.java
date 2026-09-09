package ch.sbb.polarion.extension.generic;

import ch.sbb.polarion.extension.generic.util.AdministrationMenuOrderRestorer;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

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

    /**
     * Restores the administration menu order, which Polarion 2606 randomizes on every restart.
     * <p>
     * This is the hook rather than {@code GenericBundleActivator} because it is the only one common to
     * every extension. {@code Bundle-Activator} is declared by fewer than half of them, and an
     * extension without one never ran the fix at all, while every extension declares a subclass of this
     * servlet in each of its webapps, declared with {@code load-on-startup} so that the container calls
     * this at webapp startup rather than on the first request under {@code /polarion/<extension>/ui/}.
     * Nothing else has to change in any extension.
     * <p>
     * It runs inline, without waiting for anything. Polarion builds its Guice injector inside
     * {@code PlatformService.start()} and only starts Tomcat afterwards, so the provider is always
     * available by the time a servlet is initialized. The work is a reflective read plus a permutation
     * of one list, and it is a no-op once any webapp has already done it.
     * <p>
     * The restorer guards its own body already, and this guard is not the same one: it also covers
     * loading and initializing that class. That is a real failure window, because the class names
     * Polarion's administration types in its signatures and creates a logger in its static initializer,
     * so a {@code NoClassDefFoundError} or {@code ExceptionInInitializerError} can be thrown at this
     * call site before the method body ever runs. Uncaught, it would leave the extension's UI servlet
     * unavailable over a cosmetic menu fix.
     */
    @Override
    public void init() throws ServletException {
        super.init();
        try {
            AdministrationMenuOrderRestorer.restoreDeclarationOrder();
        } catch (Exception | LinkageError e) {
            logger.warn("Could not restore the administration menu order", e);
        }
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
            serveResource(response, "/" + resourceUri);
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
