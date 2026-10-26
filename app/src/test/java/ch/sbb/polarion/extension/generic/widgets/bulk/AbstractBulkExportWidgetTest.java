package ch.sbb.polarion.extension.generic.widgets.bulk;

import com.polarion.alm.server.api.model.rp.widget.BottomQueryLinksBuilder;
import com.polarion.alm.shared.api.SharedContext;
import com.polarion.alm.shared.api.model.PrototypeEnum;
import com.polarion.alm.shared.api.model.rp.parameter.CompositeParameter;
import com.polarion.alm.shared.api.model.rp.parameter.DataSetParameter;
import com.polarion.alm.shared.api.model.rp.parameter.FieldsParameter;
import com.polarion.alm.shared.api.model.rp.parameter.IntegerParameter;
import com.polarion.alm.shared.api.model.rp.parameter.ParameterFactory;
import com.polarion.alm.shared.api.model.rp.parameter.RichPageParameter;
import com.polarion.alm.shared.api.model.rp.parameter.SortingParameter;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetContext;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetRenderingContext;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.SharedLocalization;
import com.polarion.alm.shared.api.utils.collections.ReadOnlyStrictMap;
import com.polarion.alm.shared.api.utils.html.HtmlFragmentBuilder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AbstractBulkExportWidgetTest {

    @Test
    void testGetParametersDefinition() {
        TestWidget widget = new TestWidget();

        ParameterFactory parameterFactory = mock(ParameterFactory.class, RETURNS_DEEP_STUBS);
        SharedLocalization localization = mock(SharedLocalization.class);
        SharedContext context = mock(SharedContext.class);

        when(parameterFactory.context()).thenReturn(context);
        when(context.localization()).thenReturn(localization);
        when(localization.getString(anyString())).thenReturn("test");

        FieldsParameter.Builder fieldsBuilder = mock(FieldsParameter.Builder.class, RETURNS_DEEP_STUBS);
        FieldsParameter fieldsParameter = mock(FieldsParameter.class, RETURNS_DEEP_STUBS);
        when(parameterFactory.fields(anyString())).thenReturn(fieldsBuilder);
        when(fieldsBuilder.build()).thenReturn(fieldsParameter);
        when(fieldsBuilder.disallowedFields(any())).thenReturn(fieldsBuilder);
        when(fieldsBuilder.dependencyTarget(anyBoolean())).thenReturn(fieldsBuilder);

        SortingParameter.Builder sortingBuilder = mock(SortingParameter.Builder.class, RETURNS_DEEP_STUBS);
        SortingParameter sortingParameter = mock(SortingParameter.class);
        when(parameterFactory.sorting(anyString())).thenReturn(sortingBuilder);
        when(sortingBuilder.build()).thenReturn(sortingParameter);

        DataSetParameter.Builder dataSetBuilder = mock(DataSetParameter.Builder.class, RETURNS_DEEP_STUBS);
        DataSetParameter dataSetParameter = mock(DataSetParameter.class);
        when(parameterFactory.dataSet(anyString())).thenReturn(dataSetBuilder);
        when(dataSetBuilder.allowedPrototypes(any(PrototypeEnum.class), any(PrototypeEnum[].class))).thenReturn(dataSetBuilder);
        when(dataSetBuilder.add(anyString(), any(RichPageParameter.class))).thenReturn(dataSetBuilder);
        when(dataSetBuilder.dependencySource(anyBoolean())).thenReturn(dataSetBuilder);
        when(dataSetBuilder.build()).thenReturn(dataSetParameter);

        IntegerParameter.Builder integerBuilder = mock(IntegerParameter.Builder.class, RETURNS_DEEP_STUBS);
        IntegerParameter integerParameter = mock(IntegerParameter.class);
        when(parameterFactory.integer(anyString())).thenReturn(integerBuilder);
        when(integerBuilder.value(anyInt())).thenReturn(integerBuilder);
        when(integerBuilder.build()).thenReturn(integerParameter);

        CompositeParameter.Builder compositeBuilder = mock(CompositeParameter.Builder.class, RETURNS_DEEP_STUBS);
        CompositeParameter compositeParameter = mock(CompositeParameter.class);
        when(parameterFactory.composite(anyString())).thenReturn(compositeBuilder);
        when(compositeBuilder.collapsedByDefault(anyBoolean())).thenReturn(compositeBuilder);
        when(compositeBuilder.add(anyString(), any(RichPageParameter.class))).thenReturn(compositeBuilder);
        when(compositeBuilder.build()).thenReturn(compositeParameter);

        ReadOnlyStrictMap<String, RichPageParameter> parameters = widget.getParametersDefinition(parameterFactory);

        assertNotNull(parameters);
        assertTrue(parameters.containsKey("dataSet"));
        assertTrue(parameters.containsKey("propertiesSidebarFields"));
        assertTrue(parameters.containsKey("advanced"));

        verify(parameterFactory, times(2)).fields(anyString());
        verify(parameterFactory).sorting(anyString());
        verify(parameterFactory, never()).bool(anyString());
        verify(parameterFactory).dataSet(anyString());
        verify(parameterFactory).integer(anyString());
        verify(parameterFactory).composite(anyString());
        verify(dataSetBuilder).allowedPrototypes(PrototypeEnum.Document);
        // The hook comes before the columns and the sorting, so that the extension's parameters are listed first
        verify(dataSetBuilder).add("extra", widget.extra);
        verify(integerBuilder).value(50);
    }

    @Test
    void testRenderHtml() {
        TestWidget widget = new TestWidget();
        RichPageWidgetRenderingContext renderingContext = mock(RichPageWidgetRenderingContext.class, RETURNS_DEEP_STUBS);
        DataSetParameter dataSetParameter = mock(DataSetParameter.class, RETURNS_DEEP_STUBS);
        FieldsParameter columnsParameter = mock(FieldsParameter.class, RETURNS_DEEP_STUBS);
        SortingParameter sortByParameter = mock(SortingParameter.class, RETURNS_DEEP_STUBS);
        CompositeParameter compositeParameter = mock(CompositeParameter.class, RETURNS_DEEP_STUBS);
        IntegerParameter topParameter = mock(IntegerParameter.class, RETURNS_DEEP_STUBS);
        InternalReadOnlyTransaction transaction = mock(InternalReadOnlyTransaction.class, RETURNS_DEEP_STUBS);

        when(renderingContext.transaction()).thenReturn(transaction);
        when(renderingContext.parameter("dataSet")).thenReturn(dataSetParameter);
        when(renderingContext.parameter("advanced")).thenReturn(compositeParameter);

        doReturn(PrototypeEnum.Document).when(dataSetParameter).prototype();
        when(dataSetParameter.get("columns")).thenReturn(columnsParameter);
        when(dataSetParameter.get("sortBy")).thenReturn(sortByParameter);
        when(compositeParameter.get("top")).thenReturn(topParameter);

        try (MockedConstruction<BottomQueryLinksBuilder> ignored = mockConstruction(BottomQueryLinksBuilder.class)) {
            String html = widget.renderHtml(renderingContext);
            assertNotNull(html);
        }
    }


    /**
     * A widget as an extension writes one: documents only, with one data set parameter of its own.
     */
    private static class TestWidget extends AbstractBulkExportWidget {
        private final RichPageParameter extra = mock(RichPageParameter.class);

        @Override
        protected @NotNull List<PrototypeEnum> getAllowedPrototypes() {
            return List.of(PrototypeEnum.Document);
        }

        @Override
        protected void addDataSetParameters(@NotNull DataSetParameter.Builder dataSet, @NotNull ParameterFactory parameterFactory) {
            dataSet.add("extra", extra);
        }

        @Override
        protected @NotNull AbstractBulkExportWidgetRenderer createRenderer(@NotNull RichPageWidgetRenderingContext renderingContext) {
            return new AbstractBulkExportWidgetRenderer(renderingContext) {
                @Override
                protected void renderWidgetAppLoader(@NotNull HtmlFragmentBuilder builder, @NotNull String shimId) {
                    // The widget app is not loaded in a unit test
                }

                @Override
                protected @NotNull String getShimClassName() {
                    return "test-BulkExportWidget";
                }
            };
        }

        @Override
        public @NotNull Iterable<String> getTags(@NotNull SharedContext context) {
            return List.of("Test");
        }

        @Override
        public @NotNull String getLabel(@NotNull SharedContext context) {
            return "Bulk Test Export";
        }

        @Override
        public @NotNull String getIcon(@NotNull RichPageWidgetContext context) {
            return "/polarion/test-app/ui/images/app-icon.svg";
        }

        @Override
        public @NotNull String getDetailsHtml(@NotNull RichPageWidgetContext context) {
            return "Exports documents";
        }
    }
}
