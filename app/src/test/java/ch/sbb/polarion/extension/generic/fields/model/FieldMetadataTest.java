package ch.sbb.polarion.extension.generic.fields.model;

import com.polarion.platform.persistence.model.IPrototype;
import com.polarion.subterra.base.data.model.ICustomField;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FieldMetadataTest {

    @Test
    void testEquals() {
        ICustomField customField = mock(ICustomField.class);
        when(customField.getId()).thenReturn("testFieldId");

        // field metadata are the same when their ID's same
        assertEquals(FieldMetadata.fromPrototype(mock(IPrototype.class), "testFieldId"), FieldMetadata.fromCustomField(customField));
        assertNotEquals(FieldMetadata.fromPrototype(mock(IPrototype.class), "testFieldId2"), FieldMetadata.fromCustomField(customField));
    }
}