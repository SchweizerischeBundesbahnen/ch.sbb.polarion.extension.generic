package ch.sbb.polarion.extension.generic.rest.controller.widgets;

import ch.sbb.polarion.extension.generic.rest.filter.Secured;
import ch.sbb.polarion.extension.generic.rest.model.widgets.BulkExportItems;
import ch.sbb.polarion.extension.generic.rest.model.widgets.BulkExportItemsRequest;
import ch.sbb.polarion.extension.generic.service.PolarionService;
import ch.sbb.polarion.extension.generic.widgets.bulk.BulkExportWidgetHelper;

import jakarta.inject.Singleton;
import jakarta.ws.rs.Path;

/**
 * The public face of the widget endpoint.
 * <p>
 * A token caller has no Polarion session, so the call runs through {@code callPrivileged}, as in the other api
 * controllers. It does not elevate anything: it runs the call as the token's user, so the widget shows the rows
 * that user may read.
 */
@Singleton
@Secured
@Path("/api")
public class BulkExportWidgetApiController extends BulkExportWidgetInternalController {

    private final PolarionService polarionService;

    public BulkExportWidgetApiController() {
        this(new BulkExportWidgetHelper(), new PolarionService());
    }

    public BulkExportWidgetApiController(BulkExportWidgetHelper bulkExportWidgetHelper, PolarionService polarionService) {
        super(bulkExportWidgetHelper);
        this.polarionService = polarionService;
    }

    @Override
    public BulkExportItems getItems(BulkExportItemsRequest request) {
        return polarionService.callPrivileged(() -> super.getItems(request));
    }
}
