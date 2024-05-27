package ch.sbb.polarion.extension.generic.fields;

import com.polarion.core.util.types.Currency;
import com.polarion.core.util.types.DateOnly;
import com.polarion.core.util.types.Text;
import com.polarion.core.util.types.TimeOnly;
import com.polarion.core.util.types.duration.DurationTime;
import com.polarion.subterra.base.data.model.IListType;
import com.polarion.subterra.base.data.model.IType;
import com.polarion.subterra.base.data.model.internal.EnumType;
import com.polarion.subterra.base.data.model.internal.ListType;
import com.polarion.subterra.base.data.model.internal.PrimitiveType;
import com.polarion.subterra.base.data.model.internal.ReferenceType;
import lombok.Getter;

import java.util.Date;
import java.util.Objects;
import java.util.stream.Stream;

@Getter
public enum FieldType {

    STRING(new PrimitiveType(String.class.getName())),
    INTEGER(new PrimitiveType(Integer.class.getName())),
    FLOAT(new PrimitiveType(Float.class.getName())),
    TEXT(new PrimitiveType(Text.class.getName())),
    RICH(new PrimitiveType(Text.class.getName(), "html")),
    ENUM(new EnumType("")),
    DATE_ONLY(new PrimitiveType(DateOnly.class.getName())),
    TIME_ONLY(new PrimitiveType(TimeOnly.class.getName())),
    DATE(new PrimitiveType(Date.class.getName())),
    BOOLEAN(new PrimitiveType(Boolean.class.getName())),
    CURRENCY(new PrimitiveType(Currency.class.getName())),
    DURATION(new PrimitiveType(DurationTime.class.getName())),
    LIST(new ListType("", null)),
    USER(new ReferenceType("User")),
    UNKNOWN(null);

    private final IType type;

    FieldType(IType type) {
        this.type = type;
    }

    public static FieldType recognize(IType type) {
        return Stream.of(values())
                .filter(fieldType -> isEquals(type, fieldType.getType()))
                .findFirst()
                .orElse(UNKNOWN);
    }

    public static IType unwrapIfListType(IType type) {
        return type instanceof IListType listType ? listType.getItemType() : type;
    }

    private static boolean isEquals(IType type1, IType type2) {
        if (type1 instanceof EnumType && type2 instanceof EnumType) {
            return true;
        } else if (type1 instanceof ListType && type2 instanceof ListType) {
            return true;
        } else {
            return Objects.equals(type1, type2);
        }
    }
}
