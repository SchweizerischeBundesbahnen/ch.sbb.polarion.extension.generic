package ch.sbb.polarion.extension.generic.widgets.bulk;

import com.polarion.alm.server.api.model.rp.widget.TableWidget;
import com.polarion.alm.shared.api.model.PrototypeEnum;
import com.polarion.alm.shared.api.model.rp.parameter.CompositeParameter;
import com.polarion.alm.shared.api.model.rp.parameter.DataSetParameter;
import com.polarion.alm.shared.api.model.rp.parameter.FieldsParameter;
import com.polarion.alm.shared.api.model.rp.parameter.IntegerParameter;
import com.polarion.alm.shared.api.model.rp.parameter.ParameterFactory;
import com.polarion.alm.shared.api.model.rp.parameter.RichPageParameter;
import com.polarion.alm.shared.api.model.rp.parameter.SortingParameter;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetRenderingContext;
import com.polarion.alm.shared.api.utils.SharedLocalization;
import com.polarion.alm.shared.api.utils.collections.ReadOnlyStrictMap;
import com.polarion.alm.shared.api.utils.collections.StrictMap;
import com.polarion.alm.shared.api.utils.collections.StrictMapImpl;
import com.polarion.reina.web.js.widgets.WidgetUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A report page widget listing what its data set finds, of which the selected items are exported by the extension.
 * <p>
 * The widget is a table: a data set with columns and sorting, a sidebar fields parameter and a top value, exactly as
 * Polarion's own table widget. What it lists and how it renders are the extension's: {@link #getAllowedPrototypes()}
 * names the prototypes, {@link #createRenderer(RichPageWidgetRenderingContext)} the React widget app the page loads.
 * Its rows come from {@code /widgets/bulk-export/items}, which the extension registers with
 * {@link ch.sbb.polarion.extension.generic.rest.controller.widgets.BulkExportWidgetInternalController} and
 * {@link ch.sbb.polarion.extension.generic.rest.controller.widgets.BulkExportWidgetApiController}.
 */
public abstract class AbstractBulkExportWidget extends TableWidget {

    public static final String PARAMETER_DATA_SET = "dataSet";
    public static final String PARAMETER_COLUMNS = "columns";
    public static final String PARAMETER_SORT_BY = "sortBy";
    public static final String PARAMETER_ADVANCED = "advanced";
    public static final String PARAMETER_TOP = "top";
    public static final int DEFAULT_TOP = 50;

    /**
     * The prototypes the data set may list, at least one.
     */
    protected abstract @NotNull List<PrototypeEnum> getAllowedPrototypes();

    /**
     * The renderer of this widget, which emits the shim the extension's widget app mounts into.
     */
    protected abstract @NotNull AbstractBulkExportWidgetRenderer createRenderer(@NotNull RichPageWidgetRenderingContext renderingContext);

    /**
     * Adds parameters of the extension's own to the data set. They are added before the columns and the sorting.
     */
    protected void addDataSetParameters(@NotNull DataSetParameter.Builder dataSet, @NotNull ParameterFactory parameterFactory) {
        // No parameters of its own by default
    }

    @NotNull
    @Override
    public ReadOnlyStrictMap<String, RichPageParameter> getParametersDefinition(@NotNull ParameterFactory parameterFactory) {
        StrictMap<String, RichPageParameter> parameters = new StrictMapImpl<>();
        SharedLocalization localization = parameterFactory.context().localization();
        FieldsParameter columns = parameterFactory.fields(localization.getString("richpages.widget.table.columns")).build();
        SortingParameter sortBy = parameterFactory.sorting(localization.getString("richpages.widget.table.sortBy")).build();
        List<PrototypeEnum> prototypes = getAllowedPrototypes();
        DataSetParameter.Builder dataSetBuilder = parameterFactory.dataSet(localization.getString("richpages.widget.table.dataSet"))
                .allowedPrototypes(prototypes.get(0), prototypes.subList(1, prototypes.size()).toArray(PrototypeEnum[]::new));
        addDataSetParameters(dataSetBuilder, parameterFactory);
        DataSetParameter dataSet = dataSetBuilder
                .add(PARAMETER_COLUMNS, columns)
                .add(PARAMETER_SORT_BY, sortBy)
                .dependencySource(true)
                .build();
        parameters.put(PARAMETER_DATA_SET, dataSet);
        parameters.put("propertiesSidebarFields", this.constructSidebarFieldsParameter(parameterFactory));
        IntegerParameter top = parameterFactory.integer(localization.getString("richpages.widget.table.top")).value(DEFAULT_TOP).build();
        CompositeParameter advanced = parameterFactory.composite(localization.getString("richpages.widget.advanced")).collapsedByDefault(true).add(PARAMETER_TOP, top).build();
        parameters.put(PARAMETER_ADVANCED, advanced);
        return parameters;
    }

    @NotNull
    private FieldsParameter constructSidebarFieldsParameter(@NotNull ParameterFactory factory) {
        FieldsParameter parameter = factory.fields(factory.context().localization().getString("richpages.widget.table.parameters.propertiesSidebarFields")).disallowedFields(WidgetUtil.getExcludedPropertiesSidebarFields()).dependencyTarget(true).build();
        parameter.defaultFields().set(WidgetUtil.getDefaultWidgetPropertiesSidebarFields());
        return parameter;
    }

    @NotNull
    @Override
    public String renderHtml(@NotNull RichPageWidgetRenderingContext renderingContext) {
        return createRenderer(renderingContext).render();
    }
}
