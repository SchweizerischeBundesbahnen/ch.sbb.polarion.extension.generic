package ch.sbb.polarion.extension.generic.fields.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Option {

    @EqualsAndHashCode.Include
    String key;
    String name;
}
