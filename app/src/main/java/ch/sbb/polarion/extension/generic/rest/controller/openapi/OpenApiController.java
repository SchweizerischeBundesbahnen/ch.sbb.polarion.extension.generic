package ch.sbb.polarion.extension.generic.rest.controller.openapi;

import ch.sbb.polarion.extension.generic.rest.controller.swagger.SwaggerDefinitionController;
import ch.sbb.polarion.extension.generic.rest.filter.Secured;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.Path;

@Secured
@Tag(name = "OpenAPI Specification")
@Path("/api/openapi.json")
public class OpenApiController extends SwaggerDefinitionController {
}
