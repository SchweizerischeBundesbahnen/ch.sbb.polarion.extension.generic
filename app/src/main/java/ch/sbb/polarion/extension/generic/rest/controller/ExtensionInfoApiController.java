package ch.sbb.polarion.extension.generic.rest.controller;

import javax.ws.rs.Path;

import ch.sbb.polarion.extension.generic.rest.filter.Secured;

@Secured
@Path("/api")
public class ExtensionInfoApiController extends ExtensionInfoInternalController {

}
