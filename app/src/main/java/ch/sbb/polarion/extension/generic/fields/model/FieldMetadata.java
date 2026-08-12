package ch.sbb.polarion.extension.generic.fields.model;

import ch.sbb.polarion.extension.generic.fields.FieldType;
import com.polarion.platform.persistence.model.IPrototype;
import com.polarion.subterra.base.data.model.ICustomField;
import com.polarion.subterra.base.data.model.IType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Accessors(chain = true)
public class FieldMetadata implements Comparable<FieldMetadata>{

    @EqualsAndHashCode.Include
    private String id;
    private String label;
    private IType type;
    private boolean custom;
    private boolean required;
    private boolean readOnly;
    private boolean multi;

    private Set<Option> options;

    @NotNull
    public static FieldMetadata fromPrototype(@NotNull IPrototype prototype, @NotNull String fieldId) {
        //currently cannot use prototype.getFieldLabel(fieldId) because of UnsupportedOperationException
        //in case of critical need either copy implementation from getFieldLabel(String) or try to instantiate
        //prototype object instance using trackerService.getDataService().createInstance(proto) and get field label on it
        return FieldMetadata.builder()
                .id(fieldId)
                .label(fieldId)
                .type(prototype.getKeyType(fieldId))
                .custom(!prototype.isKeyDefined(fieldId))
                .required(prototype.isKeyRequired(fieldId))
                .readOnly(prototype.isKeyReadOnly(fieldId))
                .multi(false)
                .build();
    }

    @NotNull
    public static  FieldMetadata fromCustomField(@NotNull ICustomField customField) {
        return FieldMetadata.builder()
                .id(customField.getId())
                .label(customField.getName())
                .type(customField.getType())
                .custom(true)
                .required(customField.isRequired())
                .multi(customField.isMulti())
                .build();
    }

    /**
     * Tells whether Polarion treats this field as a rich text field.
     * <p>
     * Polarion regards a Text field as rich text unless it is a custom field which doesn't declare the 'html' subtype.
     * Built-in Text fields like 'description' declare no subtype at all, so they are rich text too. This mirrors
     * {@code com.polarion.alm.tracker.internal.TypeHelper#isRichText(IType)} for built-in fields and
     * {@code #isRichTextCustomField(IType)} for custom ones, which Polarion dispatches by {@code isKeyDefined(fieldId)},
     * the same predicate {@link #fromPrototype(IPrototype, String)} uses to set {@link #custom}.
     * </p>
     * Written as plain text, such a field looks unchanged in Polarion but loses its formatting in follow-up operations
     * like a ReqIF export.
     *
     * @return true if a value of this field must be stored as {@code text/html}, false if as {@code text/plain}
     */
    public boolean isRichText() {
        return FieldType.RICH.getType().equals(type) || (FieldType.TEXT.getType().equals(type) && !custom);
    }

    /**
     * Tells whether this field holds a text value, no matter whether it is rich text or plain text.
     *
     * @return true if a value of this field must be stored as a {@link com.polarion.core.util.types.Text} instance
     */
    public boolean isTextType() {
        return FieldType.TEXT.getType().equals(type) || FieldType.RICH.getType().equals(type);
    }

    @Override
    public int compareTo(@NotNull FieldMetadata o) {
        return id.compareTo(o.getId());
    }
}
