package ch.sbb.polarion.extension.generic.fields;

import ch.sbb.polarion.extension.generic.fields.converters.ChainConverter;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import com.polarion.alm.shared.util.Pair;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.platform.core.IPlatform;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.IEnumeration;
import com.polarion.platform.persistence.spi.EnumOption;
import com.polarion.subterra.base.data.model.internal.EnumType;
import com.polarion.subterra.base.data.model.internal.ListType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class CustomFieldEnumConverterTest {

    public static final String YES_NO_ENUM_ID = "yes_no";
    public static final Pair<String, String> YES = Pair.of("yes", "Ja");
    public static final Pair<String, String> NO = Pair.of("no", "Nein");

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    MockedStatic<PlatformContext> mockPlatformContext;

    @BeforeEach
    void setup() {
        IPlatform platform = mock(IPlatform.class);
        mockPlatformContext.when(PlatformContext::getPlatform).thenReturn(platform);

        IDataService dataService = mock(IDataService.class);
        lenient().when(platform.lookupService(IDataService.class)).thenReturn(dataService);

        ITrackerService trackerService = mock(ITrackerService.class);
        lenient().when(platform.lookupService(ITrackerService.class)).thenReturn(trackerService);
        lenient().when(trackerService.getDataService()).thenReturn(dataService);

        IEnumeration<IEnumOption> enumeration = mock(IEnumeration.class);
        lenient().when(dataService.getEnumerationForEnumId(any(), any())).thenReturn(enumeration);

        List<IEnumOption> allOptions = new ArrayList<>();
        allOptions.add(new EnumOption(YES_NO_ENUM_ID, YES.left(), YES.right(), 1, true));
        allOptions.add(new EnumOption(YES_NO_ENUM_ID, NO.left(), NO.right(), 2, false));
        lenient().when(enumeration.getAllOptions()).thenReturn(allOptions);
    }

    @AfterEach
    void cleanup() {
        mockPlatformContext.close();
    }

    @Test
    void testGetEnumOptionById() {
        FieldMetadata fieldMetadata = ConverterTestUtils.getWorkItemCustomField();

        assertEquals(new EnumOption(YES_NO_ENUM_ID, YES.left()), ConverterTestUtils.process(YES.left(), fieldMetadata));
        assertEquals(new EnumOption(YES_NO_ENUM_ID, NO.left()), ConverterTestUtils.process(NO.left(), fieldMetadata));

    }

    @Test
    void testGetEnumOptionByValue() {
        FieldMetadata fieldMetadata = ConverterTestUtils.getWorkItemCustomField();

        assertEquals(new EnumOption(YES_NO_ENUM_ID, YES.left()), ConverterTestUtils.process(YES.left(), fieldMetadata));
        assertEquals(new EnumOption(YES_NO_ENUM_ID, NO.left()), ConverterTestUtils.process(NO.left(), fieldMetadata));
    }

    @Test
    void testEmptyValueForRequiredField() {
        FieldMetadata fieldMetadata = ConverterTestUtils.getWorkItemCustomField();
        fieldMetadata.setRequired(true);

        assertEquals(new EnumOption(YES_NO_ENUM_ID, YES.left()), ConverterTestUtils.process("", fieldMetadata)); // default enum value should be used if field is required
    }

    @Test
    void testEmptyValueForNotRequiredField() {
        FieldMetadata fieldMetadata = ConverterTestUtils.getWorkItemCustomField();

        assertNull(ConverterTestUtils.process("", fieldMetadata)); // nothing should be used if field is not required
    }

    @Test
    void testUnsupportedValue() {
        FieldMetadata fieldMetadata = ConverterTestUtils.getWorkItemCustomField();

        assertThrows(IllegalArgumentException.class,
                () -> ConverterTestUtils.process("unknown", fieldMetadata),
                "Unsupported value 'unknown' for enum ''");
    }

    @Test
    void testGetEnumOptionByMapping() {
        FieldMetadata fieldMetadata = ConverterTestUtils.getWorkItemCustomField();
        ConverterContext context = ConverterTestUtils.createContext(Map.of(
                "someCustomField", Map.of(
                        "yes", "Yep,Sure ,  Of course ",
                        "maybe", "Probably"
                )), null);

        //test custom values
        assertEquals(new EnumOption(YES_NO_ENUM_ID, YES.left()), ConverterTestUtils.process("Yep", context, fieldMetadata));

        //extra spaces cleaned before matching
        assertEquals(new EnumOption(YES_NO_ENUM_ID, YES.left()), ConverterTestUtils.process("Sure", context, fieldMetadata));
        assertEquals(new EnumOption(YES_NO_ENUM_ID, YES.left()), ConverterTestUtils.process("Of course", context, fieldMetadata));

        //default key/value still used for non-mapped options
        assertEquals(new EnumOption(YES_NO_ENUM_ID, NO.left()), ConverterTestUtils.process("Nein", context, fieldMetadata));

        assertEquals(new EnumOption(YES_NO_ENUM_ID, YES.left()), ConverterTestUtils.process("yes", context, fieldMetadata));

        //default key/value option identification hidden by the implicit mapping
        assertThrows(IllegalArgumentException.class,
                () -> ConverterTestUtils.process("Ja", context, fieldMetadata),
                "Unsupported value 'Ja' for enum ''");

        //just an unknown option
        assertThrows(IllegalArgumentException.class,
                () -> ConverterTestUtils.process("Probably", context, fieldMetadata),
                "Unsupported value 'Probably' for enum ''");

        //empty/blank values we treat as no-mapping
        ConverterContext noMappingContext = ConverterTestUtils.createContext(Map.of(
                "someCustomField", Map.of(
                        "yes", "  ",
                        "no", ""
                )), null);
        assertEquals(new EnumOption(YES_NO_ENUM_ID, YES.left()), ConverterTestUtils.process("Ja", noMappingContext, fieldMetadata));
        assertEquals(new EnumOption(YES_NO_ENUM_ID, NO.left()), ConverterTestUtils.process("Nein", noMappingContext, fieldMetadata));
    }

    @Test
    @SuppressWarnings("rawtypes")
    void testFindConverterByListImplementations() {
        IConverter arrayListConverter = IConverter.getSuitableConverter(new ArrayList<>(), FieldType.LIST.getType());
        assertNotNull(arrayListConverter);
        assertEquals(arrayListConverter, IConverter.getSuitableConverter(new LinkedList<>(), FieldType.LIST.getType()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetEnumOptionList() {
        FieldMetadata fieldMetadata = ConverterTestUtils.getWorkItemCustomField().setType(new ListType("", new EnumType(YES_NO_ENUM_ID)));

        assertTrue(Arrays.asList(YES.left(), NO.left())
                .containsAll((List<String>) ConverterTestUtils.process(Arrays.asList(YES.left(), NO.left()), fieldMetadata)));
    }

    @Test
    @SuppressWarnings("rawtypes")
    void testFindConverterByCSVToListImplementations() {
        IConverter stringListConverter = IConverter.getSuitableConverter("yes,no", FieldType.LIST.getType());
        assertNotNull(stringListConverter);
        assertInstanceOf(ChainConverter.class, stringListConverter);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetMultiValueEnumOptionList() {
        FieldMetadata fieldMetadata = ConverterTestUtils.getWorkItemCustomField().setType(new ListType("", new EnumType(YES_NO_ENUM_ID)));

        assertTrue(Arrays.asList(YES.left(), NO.left())
                .containsAll((List<String>) ConverterTestUtils.process("yes,no", fieldMetadata)));
        assertTrue(Arrays.asList(YES.left(), NO.left())
                .containsAll((List<String>) ConverterTestUtils.process("Ja,no", fieldMetadata)));
        assertTrue(Arrays.asList(NO.left(), YES.left())
                .containsAll((List<String>) ConverterTestUtils.process("Nein,Ja", fieldMetadata)));
    }
}
