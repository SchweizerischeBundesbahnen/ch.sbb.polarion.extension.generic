/*
 * Copyright 2020 Polarion AG
 */
package ch.sbb.polarion.extension.generic.rest.controller;

import java.util.List;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.jetbrains.annotations.NotNull;

import com.polarion.core.config.Configuration;

import io.swagger.v3.jaxrs2.integration.resources.BaseOpenApiResource;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Hidden
@Path("/swagger/definition.json")
public class SwaggerDefinitionController extends BaseOpenApiResource {

    public static final String BEARER_AUTH = "bearerAuth";

    @Context
    private ServletConfig config;
    @Context
    private Application application;

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Operation(hidden = true)
    @NotNull
    public Response openApi(@NotNull @Context HttpHeaders headers, @NotNull @Context UriInfo uriInfo) throws Exception {
        SwaggerConfiguration swaggerConfig = new SwaggerConfiguration()
                .openAPI(createOpenApi(uriInfo))
                .prettyPrint(true)
                .resourcePackages(Set.of("io.swagger.sample.resource")); // must be set to remove /application.wadl
        setOpenApiConfiguration(swaggerConfig);

        return super.getOpenApi(headers, config, application, uriInfo, "json");
    }

    @NotNull
    private OpenAPI createOpenApi(@NotNull UriInfo uriInfo) {
        OpenAPI openApiSettings = new OpenAPI();
        openApiSettings.info(createInfo());
        openApiSettings.setServers(createServers(uriInfo));
        openApiSettings.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
        openApiSettings.components(createComponents());
        return openApiSettings;
    }

    @NotNull
    private Info createInfo() {
        return new Info()
                .title("REST API");
    }

    @NotNull
    private List<Server> createServers(@NotNull UriInfo uriInfo) {
        Server defaultServer = new Server();
        String url = UriBuilder.fromUri(Configuration.getInstance().rest().baseURL().toString()).path(uriInfo.getBaseUri().getPath()).build().toString();
        defaultServer.setUrl(url);
        return List.of(defaultServer);
    }

    @NotNull
    private Components createComponents() {
        final Components components = new Components();
        components.addSecuritySchemes(BEARER_AUTH, getBearerScheme());
        return components;
    }

    @NotNull
    private SecurityScheme getBearerScheme() {
        return new SecurityScheme()
                .name(BEARER_AUTH)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
    }

}
