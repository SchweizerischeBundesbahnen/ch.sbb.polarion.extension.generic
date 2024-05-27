package ch.sbb.polarion.extension.generic;

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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public abstract class GenericUiServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger(GenericUiServlet.class);

    @Serial
    private static final long serialVersionUID = 4323903250755251706L;

    protected final String webAppName;

    protected GenericUiServlet(String webAppName) {
        this.webAppName = webAppName;
    }

    @VisibleForTesting
    static void setContentType(@NotNull String uri, @NotNull HttpServletResponse response) {
        if (uri.endsWith(".js")) {
            response.setContentType("text/javascript");
        } else if (uri.endsWith(".html")) {
            response.setContentType("text/html");
        } else if (uri.endsWith(".png")) {
            response.setContentType("image/png");
        } else if (uri.endsWith(".css")) {
            response.setContentType("text/css");
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String uri = request.getRequestURI();
        String relativeUri = uri.substring("/polarion/".length());
        try {
            if (relativeUri.startsWith(webAppName + "/ui/generic/")) {
                serveGenericResource(response, relativeUri.substring((webAppName + "/ui/generic/").length()));
            } else if (relativeUri.startsWith(webAppName + "/ui/")) {
                serveResource(response, relativeUri.substring((webAppName + "/ui").length()));
            }
        } catch (IOException e) {
            logger.error("Cannot copy resource '" + relativeUri + "': " + e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error by getting resource '" + relativeUri + "': " + e.getMessage(), e);
            throw new ServletException(e.getMessage(), e);
        }
    }

    @VisibleForTesting
    void serveGenericResource(@NotNull HttpServletResponse response, @NotNull String uri) throws IOException, URISyntaxException {
        final URI currentJar = GenericUiServlet.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        final String path = new File(currentJar).getPath();
        try (ZipFile zipFile = new ZipFile(path)) {
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
