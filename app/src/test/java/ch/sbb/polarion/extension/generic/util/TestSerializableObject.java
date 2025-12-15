package ch.sbb.polarion.extension.generic.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test class for verifying serialization/deserialization in BundleJarsPrioritizingRunnable.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestSerializableObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private String stringField;
    private int intField;
    private Integer integerField;
    private long longField;
    private double doubleField;
    private boolean booleanField;

    private List<String> stringList;
    private Set<Integer> integerSet;
    private Map<String, Object> nestedMap;

    private LocalDate localDate;
    private LocalDateTime localDateTime;

    private TestNestedObject nestedObject;
    private List<TestNestedObject> nestedObjectList;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestNestedObject implements Serializable {
        private static final long serialVersionUID = 1L;

        private String name;
        private int value;
        private List<String> tags;
    }
}
