package ch.sbb.polarion.extension.generic.jaxb;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestObject {
    private String testKey;
    private Integer testValue;
}
