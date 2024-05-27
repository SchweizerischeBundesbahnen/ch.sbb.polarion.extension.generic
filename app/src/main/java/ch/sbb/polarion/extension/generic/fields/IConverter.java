package ch.sbb.polarion.extension.generic.fields;

import ch.sbb.polarion.extension.generic.fields.converters.ChainConverter;
import ch.sbb.polarion.extension.generic.fields.converters.DateToDateOnlyConverter;
import ch.sbb.polarion.extension.generic.fields.converters.DateToTimeOnlyConverter;
import ch.sbb.polarion.extension.generic.fields.converters.DoubleToCurrencyConverter;
import ch.sbb.polarion.extension.generic.fields.converters.DoubleToIntegerConverter;
import ch.sbb.polarion.extension.generic.fields.converters.DoubleToStringConverter;
import ch.sbb.polarion.extension.generic.fields.converters.ListConverter;
import ch.sbb.polarion.extension.generic.fields.converters.StringCSVToListConverter;
import ch.sbb.polarion.extension.generic.fields.converters.StringToBooleanConverter;
import ch.sbb.polarion.extension.generic.fields.converters.StringToCurrencyConverter;
import ch.sbb.polarion.extension.generic.fields.converters.StringToDateConverter;
import ch.sbb.polarion.extension.generic.fields.converters.StringToDoubleConverter;
import ch.sbb.polarion.extension.generic.fields.converters.StringToDurationConverter;
import ch.sbb.polarion.extension.generic.fields.converters.StringToEnumOptionConverter;
import ch.sbb.polarion.extension.generic.fields.converters.StringToFloatConverter;
import ch.sbb.polarion.extension.generic.fields.converters.StringToRichConverter;
import ch.sbb.polarion.extension.generic.fields.converters.StringToTextConverter;
import ch.sbb.polarion.extension.generic.fields.converters.StringToUserConverter;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import com.polarion.alm.shared.util.Pair;
import com.polarion.subterra.base.data.model.IType;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public interface IConverter<A, B> {

    Map<Pair<Class<?>, FieldType>, IConverter<?, ?>> CONVERTERS_REGISTRY = Map.ofEntries(
            Map.entry(Pair.of(String.class, FieldType.TEXT), new StringToTextConverter()),
            Map.entry(Pair.of(String.class, FieldType.RICH), new StringToRichConverter()),
            Map.entry(Pair.of(Double.class, FieldType.TEXT), new ChainConverter(new DoubleToStringConverter(), new StringToTextConverter())),
            Map.entry(Pair.of(Double.class, FieldType.RICH), new ChainConverter(new DoubleToStringConverter(), new StringToRichConverter())),

            Map.entry(Pair.of(Double.class, FieldType.STRING), new DoubleToStringConverter()),
            Map.entry(Pair.of(String.class, FieldType.FLOAT), new StringToFloatConverter()),
            Map.entry(Pair.of(String.class, FieldType.DURATION), new StringToDurationConverter()),

            Map.entry(Pair.of(Double.class, FieldType.INTEGER), new DoubleToIntegerConverter()),
            Map.entry(Pair.of(String.class, FieldType.INTEGER), new ChainConverter(new StringToDoubleConverter(), new DoubleToIntegerConverter())),

            Map.entry(Pair.of(Double.class, FieldType.CURRENCY), new DoubleToCurrencyConverter()),
            Map.entry(Pair.of(String.class, FieldType.CURRENCY), new StringToCurrencyConverter()),

            Map.entry(Pair.of(Date.class, FieldType.DATE_ONLY), new DateToDateOnlyConverter()),
            Map.entry(Pair.of(String.class, FieldType.DATE_ONLY), new ChainConverter(new StringToDateConverter(), new DateToDateOnlyConverter())),

            Map.entry(Pair.of(String.class, FieldType.DATE), new StringToDateConverter()),

            Map.entry(Pair.of(String.class, FieldType.BOOLEAN), new StringToBooleanConverter()),

            Map.entry(Pair.of(Date.class, FieldType.TIME_ONLY), new DateToTimeOnlyConverter()),
            Map.entry(Pair.of(String.class, FieldType.TIME_ONLY), new ChainConverter(new StringToDateConverter(), new DateToTimeOnlyConverter())),

            Map.entry(Pair.of(String.class, FieldType.USER), new StringToUserConverter()),

            Map.entry(Pair.of(String.class, FieldType.ENUM), new StringToEnumOptionConverter()),
            Map.entry(Pair.of(Double.class, FieldType.ENUM), new ChainConverter(new DoubleToStringConverter(), new StringToEnumOptionConverter())),

            Map.entry(Pair.of(List.class, FieldType.LIST), new ListConverter()),
            Map.entry(Pair.of(String.class, FieldType.LIST), new ChainConverter(new StringCSVToListConverter(), new ListConverter()))
    );

    @SuppressWarnings("rawtypes")
    static IConverter getSuitableConverter(@NotNull Object initialValue, @NotNull IType type) {
        return getSuitableConverter(initialValue.getClass(), type);
    }

    @SuppressWarnings("rawtypes")
    static IConverter getSuitableConverter(@NotNull Class valueClass, @NotNull IType type) {
        FieldType fieldType = FieldType.recognize(type);
        //find not just direct instances but the inherited types too (e.g. ArrayList -> List)
        return CONVERTERS_REGISTRY.keySet().stream()
                .filter(pair -> pair.left().isAssignableFrom(valueClass) && Objects.equals(pair.right(), fieldType)).findFirst()
                .map(CONVERTERS_REGISTRY::get).orElse(null);
    }

    B convert(@NotNull A initialValue, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata);

    A convertBack(@NotNull B value, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata);

}
