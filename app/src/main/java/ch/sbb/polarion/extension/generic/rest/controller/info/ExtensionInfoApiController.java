package ch.sbb.polarion.extension.generic.rest.controller.info;

import javax.inject.Singleton;
import javax.ws.rs.Path;

import ch.sbb.polarion.extension.generic.rest.filter.Secured;

@Singleton
@Secured
@Path("/api")
public class ExtensionInfoApiController extends ExtensionInfoInternalController {

}
