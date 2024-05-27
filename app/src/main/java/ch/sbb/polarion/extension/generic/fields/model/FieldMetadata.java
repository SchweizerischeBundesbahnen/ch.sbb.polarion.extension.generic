package ch.sbb.polarion.extension.generic.fields.model;

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

    @Override
    public int compareTo(@NotNull FieldMetadata o) {
        return id.compareTo(o.getId());
    }
}
