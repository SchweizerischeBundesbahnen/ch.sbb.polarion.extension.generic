package ch.sbb.polarion.extension.generic.rest.filter;

import com.polarion.core.config.Configuration;
import com.polarion.core.util.logging.Logger;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.net.URI;
import java.util.Collections;
import java.util.Set;

@Provider
@PreMatching
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final Logger log = Logger.getLogger(CorsFilter.class);
    public static final String ORIGIN = "Origin";

    public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    public static final String ALLOWED_METHODS = String.join(", ", HttpMethod.GET, HttpMethod.HEAD, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.OPTIONS, HttpMethod.PATCH);
    public static final String ALLOWED_HEADERS = String.join(", ", ORIGIN, "X-Requested-With", "X-Requested-By", HttpHeaders.ACCEPT, HttpHeaders.CONTENT_TYPE, HttpHeaders.AUTHORIZATION);

    @Getter
    @Setter
    @Context
    private UriInfo uriInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (isCorsRequest(requestContext)) {
            if (isPreflightRequest(requestContext)) {
                handlePreflightRequest(requestContext);
            } else {
                checkRequestOrigin(requestContext);
            }
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        if (isCorsRequest(requestContext)) {
            final String origin = getOrigin(requestContext);
            if (isOriginAllowed(origin)) {
                responseContext.getHeaders().put(ACCESS_CONTROL_ALLOW_ORIGIN, Collections.singletonList(origin));
            }
        }
    }

    private void handlePreflightRequest(@NotNull ContainerRequestContext requestContext) {
        final String origin = getOrigin(requestContext);

        final Response response = Response.ok()
                .header(ACCESS_CONTROL_ALLOW_ORIGIN, origin) // provide the incoming Origin instead of "*"
                .header(HttpHeaders.VARY, ORIGIN) // it is supposed that if Access-Control-Allow-Origin returns a specific Origin, then there must also be Vary: Origin
                .header(ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.TRUE.toString())
                .header(ACCESS_CONTROL_ALLOW_HEADERS, ALLOWED_HEADERS)
                .header(ACCESS_CONTROL_ALLOW_METHODS, ALLOWED_METHODS)
                .build();

        requestContext.abortWith(response);
    }

    private void checkRequestOrigin(@NotNull ContainerRequestContext requestContext) {
        final String origin = getOrigin(requestContext);

        if (!isOriginAllowed(origin)) {
            String errorMessage = "Origin '" + origin + "' is not allowed.";
            log.warn("Cors validation failure: " + errorMessage);
            Response response = Response.status(Response.Status.FORBIDDEN)
                    .entity(errorMessage)
                    .build();
            requestContext.abortWith(response);
        }
    }

    private boolean isCorsEnabled() {
        boolean corsEnabled = !getCorsAllowedOrigins().isEmpty();
        if (!corsEnabled) {
            log.info("CORS allowed origins list is empty -> CORS is not enabled");
        }
        return corsEnabled;
    }

    private boolean isOriginAllowed(@Nullable String origin) {
        return isCorsEnabled() && (getCorsAllowedOrigins().contains("*") || isOriginContained(origin));
    }

    private boolean isOriginContained(@Nullable String origin) {
        if (origin == null) {
            return false;
        }

        Set<String> corsAllowedOrigins = getCorsAllowedOrigins();
        URI uriOrigin = URI.create(origin);

        return corsAllowedOrigins.stream()
                .map(URI::create)
                .anyMatch(corsAllowedOrigin -> isSameOrigin(uriOrigin, corsAllowedOrigin));
    }

    private boolean isCorsRequest(@NotNull ContainerRequestContext requestContext) {
        String origin = getOrigin(requestContext);
        return origin != null && !isSameOrigin(origin);
    }

    private static @Nullable String getOrigin(@NotNull ContainerRequestContext requestContext) {
        return requestContext.getHeaderString(ORIGIN);
    }

    private boolean isPreflightRequest(@NotNull ContainerRequestContext requestContext) {
        return isCorsRequest(requestContext) && HttpMethod.OPTIONS.equalsIgnoreCase(requestContext.getMethod());
    }

    private @NotNull Set<String> getCorsAllowedOrigins() {
        Set<String> corsAllowedOrigins = Configuration.getInstance().rest().corsAllowedOrigins();
        if (!corsAllowedOrigins.isEmpty()) {
            corsAllowedOrigins.add(Configuration.getInstance().getBaseURL().toString());
        }
        return corsAllowedOrigins;
    }

    private boolean isSameOrigin(@NotNull String origin) {
        URI uriServer = uriInfo.getRequestUri();
        URI uriOrigin = URI.create(origin);
        return isSameOrigin(uriServer, uriOrigin);
    }

    private boolean isSameOrigin(@NotNull URI uri1, @NotNull URI uri2) {
        return uri1.getScheme().equals(uri2.getScheme()) &&
                uri1.getHost().equals(uri2.getHost()) &&
                (uri1.getPort() == uri2.getPort());
    }
}