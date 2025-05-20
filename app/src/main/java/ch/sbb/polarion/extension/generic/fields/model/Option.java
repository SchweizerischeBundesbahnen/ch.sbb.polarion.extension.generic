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
    String iconUrl;

    /**
     * Constructor for backward compatibility when iconUrl is not used.
     */
    public Option(String key, String name) {
        this.key = key;
        this.name = name;
    }

}
