package ch.sbb.polarion.extension.generic.rest.filter;

import com.polarion.core.config.Configuration;
import com.polarion.core.config.IConfiguration;
import com.polarion.core.config.IRestConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorsFilterTest {

    public static final String LOCALHOST_8080 = "http://localhost:8080";
    public static final String LOCALHOST_1111 = "http://localhost:1111";

    @Mock
    private ContainerRequestContext requestContext;
    @Mock
    private ContainerResponseContext responseContext;
    @Mock
    private IRestConfiguration restConfiguration;
    @Mock
    MockedStatic<Configuration> configurationMockedStatic;

    @BeforeEach
    void setUp() throws MalformedURLException {
        IConfiguration configuration = mock(IConfiguration.class);
        configurationMockedStatic.when(Configuration::getInstance).thenReturn(configuration);
        lenient().when(configuration.rest()).thenReturn(restConfiguration);
        lenient().when(configuration.getBaseURL()).thenReturn(URI.create(LOCALHOST_8080).toURL());
    }

    @AfterEach
    void tearDown() {
        configurationMockedStatic.close();
    }

    @Mock
    private UriInfo uriInfo;

    @Test
    void handlePreflightRequest() throws URISyntaxException {
        when(uriInfo.getRequestUri()).thenReturn(new URI(LOCALHOST_8080 + "/polarion/some-extension"));
        when(requestContext.getHeaderString(CorsFilter.ORIGIN)).thenReturn(LOCALHOST_1111);
        when(requestContext.getMethod()).thenReturn(HttpMethod.OPTIONS);

        CorsFilter corsFilter = new CorsFilter();
        corsFilter.setUriInfo(uriInfo);
        corsFilter.filter(requestContext);

        ArgumentCaptor<Response> argument = ArgumentCaptor.forClass(Response.class);
        verify(requestContext, times(1)).abortWith(argument.capture());
        Response response = argument.getValue();
        assertEquals(HttpStatus.OK.value(), response.getStatus());

        MultivaluedMap<String, Object> headers = response.getHeaders();
        assertEquals(List.of(LOCALHOST_1111), headers.get(CorsFilter.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertEquals(List.of("Origin"), headers.get(HttpHeaders.VARY));
        assertEquals(List.of("true"), headers.get(CorsFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        assertEquals(List.of("Origin, X-Requested-With, X-Requested-By, Accept, Content-Type, Authorization"), headers.get(CorsFilter.ACCESS_CONTROL_ALLOW_HEADERS));
        assertEquals(List.of("GET, HEAD, POST, PUT, DELETE, OPTIONS, PATCH"), headers.get(CorsFilter.ACCESS_CONTROL_ALLOW_METHODS));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", LOCALHOST_8080, "http://localhost:2222,http://localhost:3333"})
    void requestOriginNotAllowed(String input) throws URISyntaxException {
        when(uriInfo.getRequestUri()).thenReturn(new URI(LOCALHOST_8080 + "/some-extension"));
        when(requestContext.getHeaderString(CorsFilter.ORIGIN)).thenReturn(LOCALHOST_1111);
        when(requestContext.getMethod()).thenReturn(HttpMethod.GET);

        // no CORS enabled
        HashSet<String> corsAllowedOrigins = new HashSet<>(Arrays.asList(input.split(",")));
        when(restConfiguration.corsAllowedOrigins()).thenReturn(corsAllowedOrigins);

        CorsFilter corsFilter = new CorsFilter();
        corsFilter.setUriInfo(uriInfo);
        corsFilter.filter(requestContext);

        ArgumentCaptor<Response> argument = ArgumentCaptor.forClass(Response.class);
        verify(requestContext, times(1)).abortWith(argument.capture());
        Response response = argument.getValue();
        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());

        assertEquals("Origin 'http://localhost:1111' is not allowed.", response.getEntity());
    }

    @ParameterizedTest
    @ValueSource(strings = {LOCALHOST_1111, "*", "http://localhost:1111,http://localhost:2222", "http://localhost:1111/"})
    void requestOriginAllowed(String input) throws URISyntaxException {
        when(uriInfo.getRequestUri()).thenReturn(new URI(LOCALHOST_8080 + "/some-extension"));
        when(requestContext.getHeaderString(CorsFilter.ORIGIN)).thenReturn(LOCALHOST_1111);
        when(requestContext.getMethod()).thenReturn(HttpMethod.GET);

        HashSet<String> corsAllowedOrigins = new HashSet<>(Arrays.asList(input.split(",")));
        when(restConfiguration.corsAllowedOrigins()).thenReturn(corsAllowedOrigins);

        CorsFilter corsFilter = new CorsFilter();
        corsFilter.setUriInfo(uriInfo);
        corsFilter.filter(requestContext);

        verify(requestContext, times(0)).abortWith(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {LOCALHOST_1111, "*", "http://localhost:1111,http://localhost:2222", "http://localhost:1111/"})
    void testContainerResponseFilter(String input) throws URISyntaxException {
        when(uriInfo.getRequestUri()).thenReturn(new URI(LOCALHOST_8080 + "/some-extension"));
        when(requestContext.getHeaderString(CorsFilter.ORIGIN)).thenReturn(LOCALHOST_1111);

        HashSet<String> corsAllowedOrigins = new HashSet<>(Arrays.asList(input.split(",")));
        when(restConfiguration.corsAllowedOrigins()).thenReturn(corsAllowedOrigins);

        MultivaluedMap<String, Object> responseHeaders = new MultivaluedHashMap<>();
        when(responseContext.getHeaders()).thenReturn(responseHeaders);

        CorsFilter corsFilter = new CorsFilter();
        corsFilter.setUriInfo(uriInfo);
        corsFilter.filter(requestContext, responseContext);

        verify(responseContext, times(1)).getHeaders();
        assertTrue(responseContext.getHeaders().containsKey(CorsFilter.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertEquals(LOCALHOST_1111, responseContext.getHeaders().getFirst(CorsFilter.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
