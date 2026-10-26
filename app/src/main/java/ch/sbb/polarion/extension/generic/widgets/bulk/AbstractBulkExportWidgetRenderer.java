package ch.sbb.polarion.extension.generic.widgets.bulk;

import ch.sbb.polarion.extension.generic.rest.model.widgets.BulkExportColumn;
import ch.sbb.polarion.extension.generic.rest.model.widgets.BulkExportWidgetDescriptor;
import ch.sbb.polarion.extension.generic.util.BundleCacheKey;
import ch.sbb.polarion.extension.generic.widgets.DataSetQueryTypeReader;
import ch.sbb.polarion.extension.generic.widgets.WidgetDescriptorSigner;
import com.polarion.alm.server.api.model.rp.widget.AbstractWidgetRenderer;
import com.polarion.alm.shared.api.model.PrototypeEnum;
import com.polarion.alm.shared.api.model.baselinecollection.BaselineCollectionReference;
import com.polarion.alm.shared.api.model.rp.parameter.CompositeParameter;
import com.polarion.alm.shared.api.model.rp.parameter.DataSet;
import com.polarion.alm.shared.api.model.rp.parameter.DataSetParameter;
import com.polarion.alm.shared.api.model.rp.parameter.Field;
import com.polarion.alm.shared.api.model.rp.parameter.FieldsParameter;
import com.polarion.alm.shared.api.model.rp.parameter.IntegerParameter;
import com.polarion.alm.shared.api.model.rp.parameter.SortingParameter;
import com.polarion.alm.shared.api.model.rp.parameter.impl.dataset.WidgetContextScope;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetCommonContext;
import com.polarion.alm.shared.api.utils.collections.IterableWithSize;
import com.polarion.alm.shared.api.utils.html.HtmlAttributesBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlFragmentBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlTagBuilder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static ch.sbb.polarion.extension.generic.widgets.bulk.AbstractBulkExportWidget.DEFAULT_TOP;
import static ch.sbb.polarion.extension.generic.widgets.bulk.AbstractBulkExportWidget.PARAMETER_ADVANCED;
import static ch.sbb.polarion.extension.generic.widgets.bulk.AbstractBulkExportWidget.PARAMETER_COLUMNS;
import static ch.sbb.polarion.extension.generic.widgets.bulk.AbstractBulkExportWidget.PARAMETER_DATA_SET;
import static ch.sbb.polarion.extension.generic.widgets.bulk.AbstractBulkExportWidget.PARAMETER_SORT_BY;
import static ch.sbb.polarion.extension.generic.widgets.bulk.AbstractBulkExportWidget.PARAMETER_TOP;

/**
 * Renders a bulk export widget as a shim which the extension's React widget app mounts into.
 * <p>
 * The table itself is built in the browser: the shim carries the widget's resolved data set, and the React app asks
 * {@code /widgets/bulk-export/items} for the rows. Only what the widget rendering context alone can answer is
 * resolved here - the query, the sorting, the column labels - and it travels signed, see
 * {@link WidgetDescriptorSigner}.
 * <p>
 * An extension names its widget app with {@link #getModuleUrl()} and {@link #getModulePath()}, and adds what that
 * app needs for its presentation with {@link #addShimAttributes(HtmlAttributesBuilder)}.
 */
public abstract class AbstractBulkExportWidgetRenderer extends AbstractWidgetRenderer {

    protected final @NotNull DataSetParameter dataSetParameter;
    private final @NotNull DataSet dataSet;
    private final @Nullable String sort;

    @Getter
    private final int topItems;
    @Getter
    private final @NotNull IterableWithSize<Field> columns;
    @Getter
    private final @NotNull PrototypeEnum itemsPrototype;

    protected AbstractBulkExportWidgetRenderer(@NotNull RichPageWidgetCommonContext context) {
        super(context);
        this.dataSetParameter = context.parameter(PARAMETER_DATA_SET);
        FieldsParameter columnsParameter = dataSetParameter.get(PARAMETER_COLUMNS);
        SortingParameter sortByParameter = dataSetParameter.get(PARAMETER_SORT_BY);
        String luceneSort = sortByParameter.asLuceneSortString();
        this.sort = luceneSort;
        this.dataSet = dataSetParameter.getFor().sort(luceneSort).revision(null);
        this.itemsPrototype = dataSetParameter.prototype();
        this.columns = columnsParameter.fields();

        CompositeParameter advanced = context.parameter(PARAMETER_ADVANCED);
        IntegerParameter top = advanced.get(PARAMETER_TOP);
        Integer topValue = top.value();
        if (topValue != null) {
            this.topItems = topValue == 0 ? Integer.MAX_VALUE : topValue;
        } else {
            this.topItems = DEFAULT_TOP;
        }
    }

    /**
     * The URL the page imports the widget app from, e.g. {@code /polarion/<extension>-app/ui/app/assets/bulk-widget.js}.
     * Its default export is called with the selector of the shim.
     */
    protected abstract @NotNull String getModuleUrl();

    /**
     * The same module in the jar, e.g. {@code webapp/<extension>-app/app/assets/bulk-widget.js}, which is what its
     * cache key is a hash of.
     */
    protected abstract @NotNull String getModulePath();

    /**
     * The class of the shim element, next to {@code sbb-ui}.
     */
    protected abstract @NotNull String getShimClassName();

    /**
     * Adds what the widget app needs to render the widget's frame before the rows arrive. Never trusted
     * server-side: everything the endpoint acts on comes out of the signed descriptor. The default is the title.
     */
    protected void addShimAttributes(@NotNull HtmlAttributesBuilder attributes) {
        attributes.byName("data-title", getItemsTitle(itemsPrototype));
    }

    /**
     * What the widget calls the items it lists. A prototype without a name of its own here is called by its own.
     */
    public @NotNull String getItemsTitle(@NotNull PrototypeEnum prototype) {
        return switch (prototype) {
            case Document -> "Documents";
            case RichPage -> "Pages";
            case TestRun -> "Test Runs";
            case BaselineCollection -> "Collections";
            default -> prototype.name();
        };
    }

    @Override
    @VisibleForTesting
    public void render(@NotNull HtmlFragmentBuilder builder) {
        if (this.topItems < 0) {
            builder.html(this.context.renderWarning(this.localization.getString("richpages.widget.table.invalidTopValue")));
            return;
        }

        String panelId = "bulk-%s".formatted(UUID.randomUUID().toString());
        String descriptor = WidgetDescriptorSigner.getInstance().encode(buildDescriptor());

        // Nothing else is put on the page for this widget. Everything it renders lives in the shadow root the
        // widget app attaches to the shim below, styled by the stylesheets that app injects into it.
        HtmlTagBuilder shim = builder.tag().div();
        // sbb-ui carries the --sbb-* design tokens, which the widget's shadow root inherits through this element:
        // the token declarations live on .sbb-ui, not on :root, so this element is what makes the widget's
        // tokens resolve on a plain Polarion page.
        shim.attributes().className(getShimClassName() + " sbb-ui").id(panelId);
        HtmlAttributesBuilder attributes = shim.attributes()
                .byName("data-descriptor", descriptor)
                .byName("data-signature", WidgetDescriptorSigner.getInstance().sign(descriptor));
        addShimAttributes(attributes);

        //language=JS
        builder.tag().script().append().javaScript("""
                import('%s?v=%s')
                    .then(module => module.default('#%s'))
                    .catch(console.error);""".formatted(getModuleUrl(), BundleCacheKey.forModule(getModulePath()), panelId));
    }

    @VisibleForTesting
    @NotNull
    public BulkExportWidgetDescriptor buildDescriptor() {
        BaselineCollectionReference collection = getCollectionReference();
        return BulkExportWidgetDescriptor.builder()
                .prototype(itemsPrototype.name())
                .query(dataSet.queryToShow())
                .sqlQuery(DataSetQueryTypeReader.isSqlQuery(dataSetParameter))
                .sort(sort)
                .top(topItems)
                // A page opened in a baseline renders its widgets at that revision. The endpoint runs in a
                // transaction of its own, which has no baseline, so the revision has to be carried explicitly.
                .revision(context.transaction().context().baselineRevision())
                .projectId(dataSetParameter.scope().projectId())
                .collectionProjectId(collection == null ? null : collection.projectId())
                .collectionId(collection == null ? null : collection.id())
                .columns(getDescriptorColumns())
                .build();
    }

    @SuppressWarnings("java:S9391") // the IterableWithSize of Polarion offers no stream
    private @NotNull List<BulkExportColumn> getDescriptorColumns() {
        List<BulkExportColumn> descriptorColumns = new ArrayList<>();
        for (Field column : columns) {
            // The label is resolved here on purpose: a field's label as the widget's column parameter knows it is
            // not reachable from the field of an item, which is all the endpoint has.
            descriptorColumns.add(new BulkExportColumn(column.id(), column.label()));
        }
        return descriptorColumns;
    }

    /**
     * The collection a page opened inside a baseline collection queries, which Polarion takes from the widget's own
     * parameter first and from the page context second.
     */
    private @Nullable BaselineCollectionReference getCollectionReference() {
        if (dataSetParameter.getWidgetContextScope() != WidgetContextScope.Collection) {
            return null;
        }
        BaselineCollectionReference reference = dataSetParameter.getCollectionReference();
        return reference != null ? reference : context.transaction().context().contextCollection();
    }
}
