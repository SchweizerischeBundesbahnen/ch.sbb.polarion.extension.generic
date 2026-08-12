package ch.sbb.polarion.extension.generic.fields.model;

import com.polarion.core.util.types.Text;
import com.polarion.platform.persistence.model.IPrototype;
import com.polarion.subterra.base.data.model.ICustomField;
import com.polarion.subterra.base.data.model.IType;
import com.polarion.subterra.base.data.model.internal.PrimitiveType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Test
    void testIsRichTextForBuiltInFields() {
        // a built-in Text field declares no subtype at all, still Polarion treats it as rich text
        assertTrue(builtInField("description", new PrimitiveType(Text.class.getName())).isRichText());
        assertTrue(builtInField("description", new PrimitiveType(Text.class.getName(), "html")).isRichText());
        assertFalse(builtInField("title", new PrimitiveType(String.class.getName())).isRichText());
    }

    @Test
    void testIsRichTextForCustomFields() {
        // a custom field is rich text only if it declares the 'html' subtype
        assertTrue(customField("customRichText", new PrimitiveType(Text.class.getName(), "html")).isRichText());
        assertFalse(customField("customPlainText", new PrimitiveType(Text.class.getName())).isRichText());
        assertFalse(customField("customString", new PrimitiveType(String.class.getName())).isRichText());
    }

    @Test
    void testIsTextType() {
        assertTrue(builtInField("description", new PrimitiveType(Text.class.getName())).isTextType());
        assertTrue(customField("customRichText", new PrimitiveType(Text.class.getName(), "html")).isTextType());
        assertFalse(customField("customString", new PrimitiveType(String.class.getName())).isTextType());
    }

    private FieldMetadata builtInField(String fieldId, IType type) {
        IPrototype prototype = mock(IPrototype.class);
        when(prototype.getKeyType(fieldId)).thenReturn(type);
        when(prototype.isKeyDefined(fieldId)).thenReturn(true);
        return FieldMetadata.fromPrototype(prototype, fieldId);
    }

    private FieldMetadata customField(String fieldId, IType type) {
        ICustomField customField = mock(ICustomField.class);
        when(customField.getId()).thenReturn(fieldId);
        when(customField.getType()).thenReturn(type);
        return FieldMetadata.fromCustomField(customField);
    }
}
