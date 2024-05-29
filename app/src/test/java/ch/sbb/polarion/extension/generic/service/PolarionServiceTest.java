package ch.sbb.polarion.extension.generic.service;

import ch.sbb.polarion.extension.generic.exception.ObjectNotFoundException;
import ch.sbb.polarion.extension.generic.fields.FieldType;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import ch.sbb.polarion.extension.generic.fields.model.Option;
import ch.sbb.polarion.extension.generic.util.AssigneeUtils;
import ch.sbb.polarion.extension.generic.util.TestUtils;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.tracker.IModuleManager;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollection;
import com.polarion.platform.IPlatformService;
import com.polarion.platform.persistence.ICustomFieldsService;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.IEnumeration;
import com.polarion.platform.persistence.model.IPObject;
import com.polarion.platform.persistence.model.IPrototype;
import com.polarion.platform.persistence.spi.EnumOption;
import com.polarion.subterra.base.data.identification.IContextId;
import com.polarion.subterra.base.data.identification.IObjectId;
import com.polarion.subterra.base.data.model.ICustomField;
import com.polarion.subterra.base.data.model.IEnumType;
import com.polarion.subterra.base.data.model.internal.EnumType;
import com.polarion.subterra.base.data.model.internal.ListType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked", "UnusedReturnValue"})
public class PolarionServiceTest {

    private static final String PROJECT_ID = "project_id";
    private static final String WI_ID = "wi_id";
    private static final String SPACE_ID = "space_id";
    private static final String DOCUMENT_NAME = "document_name";
    public static final String REVISION = "1000";
    private static final String COLLECTION_ID = "1";
    private static final String POLARION_PRODUCT_NAME = "POLARION";
    private static final String POLARION_VERSION = "1.0.0";

    @Mock
    private IProjectService projectService;

    @Mock
    private ITrackerService trackerService;

    @Mock
    private IPlatformService platformService;

    @Mock
    private IDataService dataService;

    @Mock
    private IModuleManager moduleManager;

    private PolarionService polarionService;

    @BeforeEach
    void init() {
        polarionService = TestUtils.mockPolarionService(trackerService, projectService, null, platformService, null);
    }

    @Test
    void testGetNotExistentProject() {
        mockProject(Boolean.FALSE);

        assertThrows(ObjectNotFoundException.class, () -> polarionService.getProject(PROJECT_ID, null));
    }

    @Test
    void testGetNotExistentWorkItem() {
        IProject project = mockProject(Boolean.TRUE);
        mockWorkItem(project, Boolean.FALSE);

        assertThrows(ObjectNotFoundException.class, () -> polarionService.getWorkItem(PROJECT_ID, WI_ID));
    }

    @Test
    void testGetNotExistentModule() {
        IProject project = mockProject(Boolean.TRUE);
        mockModule(project, Boolean.FALSE);

        assertThrows(ObjectNotFoundException.class, () -> polarionService.getModule(PROJECT_ID, SPACE_ID, DOCUMENT_NAME));
    }

    @Test
    void testGetNotExistentCollection() {
        try (MockedStatic<TransactionalExecutor> transactionalExecutorMockedStatic = mockStatic(TransactionalExecutor.class)) {
            mockProject(Boolean.TRUE);
            IBaselineCollection collection = mockCollection(Boolean.FALSE);
            transactionalExecutorMockedStatic.when(() -> TransactionalExecutor.executeSafelyInReadOnlyTransaction(any())).thenReturn(collection);

            assertThrows(ObjectNotFoundException.class, () -> polarionService.getCollection(PROJECT_ID, COLLECTION_ID));
        }
    }

    @Test
    void testGetProject() {
        mockProject(Boolean.TRUE);

        assertNotNull(polarionService.getProject(PROJECT_ID));
    }

    @Test
    void testGetProjectWithRevision() {
        mockProject(Boolean.TRUE);

        assertNotNull(polarionService.getProject(PROJECT_ID, REVISION));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> polarionService.getProject("", REVISION));
        assertEquals("Parameter 'projectId' should be provided", exception.getMessage());
    }

    @Test
    void testRestrictNullForRequiredField() {
        IPObject ipObject = mock(IPObject.class);
        IPrototype prototype = mock(IPrototype.class);

        when(ipObject.getPrototype()).thenReturn(prototype);
        when(prototype.isKeyDefined(any())).thenReturn(true);

        try (MockedStatic<FieldMetadata> mockMetadata = mockStatic(FieldMetadata.class)) {
            mockMetadata.when(() -> FieldMetadata.fromPrototype(any(), any())).thenReturn(new FieldMetadata().setId("fieldId").setRequired(true));
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> polarionService.setFieldValue(ipObject, "fieldId", null));
            assertEquals("Cannot set empty value to the required field", exception.getMessage());
        }
    }

    @Test
    void testGetWorkItem() {
        IProject project = mockProject(Boolean.TRUE);
        mockWorkItem(project, Boolean.TRUE);

        assertNotNull(polarionService.getWorkItem(PROJECT_ID, WI_ID));
    }

    @Test
    void testGetWorkItemWithRevision() {
        IProject project = mockProject(Boolean.TRUE);
        mockWorkItem(project, Boolean.TRUE);

        assertNotNull(polarionService.getWorkItem(PROJECT_ID, WI_ID, REVISION));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> polarionService.getWorkItem("", WI_ID, REVISION));
        assertEquals("Parameter 'projectId' should be provided", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class,
                () -> polarionService.getWorkItem(PROJECT_ID, "", REVISION));
        assertEquals("Parameter 'workItemId' should be provided", exception.getMessage());
    }

    @Test
    void testGetModule() {
        IProject project = mockProject(Boolean.TRUE);
        mockModule(project, Boolean.TRUE);

        assertNotNull(polarionService.getModule(PROJECT_ID, SPACE_ID, DOCUMENT_NAME));
    }

    @Test
    void testGetModuleWithRevision() {
        IProject project = mockProject(Boolean.TRUE);
        mockModule(project, Boolean.TRUE);

        assertNotNull(polarionService.getModule(PROJECT_ID, SPACE_ID, DOCUMENT_NAME, REVISION));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> polarionService.getModule(PROJECT_ID, "", DOCUMENT_NAME, REVISION));
        assertEquals("Parameter 'spaceId' should be provided", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class,
                () -> polarionService.getModule(PROJECT_ID, SPACE_ID, "", REVISION));
        assertEquals("Parameter 'documentName' should be provided", exception.getMessage());
    }

    @Test
    void testGetCollection() {
        try (MockedStatic<TransactionalExecutor> transactionalExecutorMockedStatic = mockStatic(TransactionalExecutor.class)) {
            mockProject(Boolean.TRUE);
            IBaselineCollection collection = mockCollection(Boolean.TRUE);
            transactionalExecutorMockedStatic.when(() -> TransactionalExecutor.executeSafelyInReadOnlyTransaction(any())).thenReturn(collection);

            assertNotNull(polarionService.getCollection(PROJECT_ID, COLLECTION_ID));
        }
    }

    @Test
    void testGetCollectionWithRevision() {
        try (MockedStatic<TransactionalExecutor> transactionalExecutorMockedStatic = mockStatic(TransactionalExecutor.class)) {
            mockProject(Boolean.TRUE);
            IBaselineCollection collection = mockCollection(Boolean.TRUE);
            transactionalExecutorMockedStatic.when(() -> TransactionalExecutor.executeSafelyInReadOnlyTransaction(any())).thenReturn(collection);

            assertNotNull(polarionService.getCollection(PROJECT_ID, COLLECTION_ID, REVISION));

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> polarionService.getCollection(PROJECT_ID, "", REVISION));
            assertEquals("Parameter 'collectionId' should be provided", exception.getMessage());
        }
    }

    @Test
    void testGetGeneralFields() {
        IDataService dataService = mock(IDataService.class);
        when(trackerService.getDataService()).thenReturn(dataService);
        IPrototype prototype = mock(IPrototype.class);
        when(prototype.getKeyNames()).thenReturn(List.of("key1", "key2", "key3"));
        when(dataService.getPrototype(anyString())).thenReturn(prototype);

        FieldMetadata metadata1 = FieldMetadata.builder().id("id1").build();
        FieldMetadata metadata2 = FieldMetadata.builder().id("id2").build();
        FieldMetadata metadata3 = FieldMetadata.builder().id("id3").build();

        try (MockedStatic<FieldMetadata> fieldMetadataStatic = mockStatic(FieldMetadata.class)) {
            fieldMetadataStatic.when(() -> FieldMetadata.fromPrototype(any(), eq("key1"))).thenReturn(metadata1);
            fieldMetadataStatic.when(() -> FieldMetadata.fromPrototype(any(), eq("key2"))).thenReturn(metadata2);
            fieldMetadataStatic.when(() -> FieldMetadata.fromPrototype(any(), eq("key3"))).thenReturn(metadata3);
            assertTrue(polarionService.getGeneralFields("protoName", mock(IContextId.class)).containsAll(List.of(metadata1, metadata2, metadata3)));
        }
    }

    @Test
    void testGetCustomFields() {
        IDataService dataService = mock(IDataService.class);
        when(trackerService.getDataService()).thenReturn(dataService);
        ICustomFieldsService customFieldsService = mock(ICustomFieldsService.class);
        ICustomField customField1 = mock(ICustomField.class);
        ICustomField customField2 = mock(ICustomField.class);
        ICustomField customField3 = mock(ICustomField.class);
        when(customFieldsService.getCustomFields(anyString(), any(), anyString())).thenReturn(List.of(customField1, customField2, customField3));
        when(dataService.getCustomFieldsService()).thenReturn(customFieldsService);

        FieldMetadata metadata1 = FieldMetadata.builder().id("id1").build();
        FieldMetadata metadata2 = FieldMetadata.builder().id("id2").build();
        FieldMetadata metadata3 = FieldMetadata.builder().id("id3").build();

        try (MockedStatic<FieldMetadata> fieldMetadataStatic = mockStatic(FieldMetadata.class)) {
            fieldMetadataStatic.when(() -> FieldMetadata.fromCustomField(eq(customField1))).thenReturn(metadata1);
            fieldMetadataStatic.when(() -> FieldMetadata.fromCustomField(eq(customField2))).thenReturn(metadata2);
            fieldMetadataStatic.when(() -> FieldMetadata.fromCustomField(eq(customField3))).thenReturn(metadata3);
            assertTrue(polarionService.getCustomFields("protoName", mock(IContextId.class), "optType").containsAll(List.of(metadata1, metadata2, metadata3)));
        }
    }

    @Test
    void testSetFieldValue() {
        IWorkItem workItem = mock(IWorkItem.class);

        // 'assignee' field processed in a special way
        try (MockedStatic<AssigneeUtils> assigneeUtilsMockedStatic = mockStatic(AssigneeUtils.class)) {
            assigneeUtilsMockedStatic.when(() -> AssigneeUtils.setAssignees(any(), any())).then(invocationOnMock -> null);
            polarionService.setFieldValue(workItem, "assignee", "value");
            verify(workItem, times(0)).getPrototype();
        }

        Map<String, Map<String, String>> enumsMapping = Map.of();
        IPrototype prototype = mock(IPrototype.class);
        when(workItem.getPrototype()).thenReturn(prototype);
        when(prototype.isKeyDefined(anyString())).thenReturn(false);
        when(prototype.isKeyDefined("genericFieldId")).thenReturn(true);
        FieldMetadata genericFieldMetadata = FieldMetadata.builder().id("genericFieldId").type(FieldType.STRING.getType()).build();
        FieldMetadata customFieldMetadata = FieldMetadata.builder().id("customFieldId").type(FieldType.STRING.getType()).custom(true).build();

        try (MockedStatic<FieldMetadata> fieldMetadataStatic = mockStatic(FieldMetadata.class)) {
            fieldMetadataStatic.when(() -> FieldMetadata.fromPrototype(any(), anyString())).thenReturn(genericFieldMetadata);
            fieldMetadataStatic.when(() -> FieldMetadata.fromCustomField(any())).thenReturn(customFieldMetadata);

            // for generic fields must be setValue() used
            polarionService.setFieldValue(workItem, "genericFieldId", "value", enumsMapping);
            verify(workItem, times(1)).setValue("genericFieldId", "value");
            verify(workItem, times(0)).setCustomField(anyString(), any());

            // for custom fields - setCustomField()
            polarionService.setFieldValue(workItem, "customFieldId", "value", enumsMapping);
            verify(workItem, times(1)).setValue(anyString(), any());
            verify(workItem, times(1)).setCustomField("customFieldId", "value");

            // nulls must be propagated for non-required fields
            polarionService.setFieldValue(workItem, "genericFieldId", null, enumsMapping);
            verify(workItem, times(1)).setValue("genericFieldId", null);

            List<String> listValue = List.of("value1", "value2");

            // primitive fields cannot receive lists
            assertThrows(ClassCastException.class,
                    () -> polarionService.setFieldValue(workItem, "genericFieldId", listValue, enumsMapping));

            // lists must be supported
            genericFieldMetadata.setType(new ListType("listTypeId", FieldType.STRING.getType()));
            polarionService.setFieldValue(workItem, "genericFieldId", listValue, enumsMapping);
            verify(workItem, times(3)).setValue(anyString(), any());
            verify(workItem, times(1)).setCustomField(anyString(), any());

            // nulls for list-typed fields must be converted to an empty list
            polarionService.setFieldValue(workItem, "genericFieldId", null, enumsMapping);
            verify(workItem, times(1)).setValue("genericFieldId", new ArrayList<>());

            // nulls in required fields
            genericFieldMetadata.setRequired(true);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> polarionService.setFieldValue(workItem, "genericFieldId", null, enumsMapping));
            assertEquals("Cannot set empty value to the required field", exception.getMessage());
        }
    }

    @Test
    void testGetFieldValue() {
        IWorkItem workItem = mock(IWorkItem.class);

        IPrototype prototype = mock(IPrototype.class);
        when(workItem.getPrototype()).thenReturn(prototype);
        when(prototype.isKeyDefined(anyString())).thenReturn(false);
        when(prototype.isKeyDefined("genericFieldId")).thenReturn(true);

        when(workItem.getValue(anyString())).thenReturn("genericValue");
        when(workItem.getCustomField(anyString())).thenReturn("customValue");

        assertEquals("genericValue", polarionService.getFieldValue(workItem, "genericFieldId"));
        assertEquals("customValue", polarionService.getFieldValue(workItem, "someCustomFieldId"));

        when(workItem.getCustomField("nullField")).thenReturn(null);
        assertNull(polarionService.getFieldValue(workItem, "nullField", Double.class));

        when(workItem.getPrototype()).thenReturn(prototype);
        when(prototype.isKeyDefined(anyString())).thenReturn(false);
        when(prototype.isKeyDefined("genericFieldId")).thenReturn(true);

        FieldMetadata genericFieldMetadata = FieldMetadata.builder().id("genericFieldId").type(new ListType("listTypeId", FieldType.STRING.getType())).build();
        FieldMetadata customFieldMetadata = FieldMetadata.builder().id("customFieldId").type(FieldType.STRING.getType()).custom(true).build();

        try (MockedStatic<FieldMetadata> fieldMetadataStatic = mockStatic(FieldMetadata.class)) {
            fieldMetadataStatic.when(() -> FieldMetadata.fromPrototype(any(), anyString())).thenReturn(genericFieldMetadata);
            fieldMetadataStatic.when(() -> FieldMetadata.fromCustomField(any())).thenReturn(customFieldMetadata);

            when(workItem.getCustomField(anyString())).thenReturn("5");
            assertEquals(5d, polarionService.getFieldValue(workItem, "intField", Double.class));

            when(workItem.getValue(anyString())).thenReturn(List.of("1", "2"));
            Object intField = polarionService.getFieldValue(workItem, "genericFieldId", String.class);
            assertEquals("1,2", intField);
        }
    }

    @Test
    void testGetOptionsForEnum() {
        IContextId contextId = mock(IContextId.class);
        assertNull(polarionService.getOptionsForEnum(FieldType.STRING.getType(), contextId));
        assertNull(polarionService.getOptionsForEnum(new ListType("listTypeId", FieldType.STRING.getType()), contextId));

        IEnumOption enumOption1 = new EnumOption("enumId1", "optionId1", "optionName1", 1, false);
        IEnumOption enumOption2 = new EnumOption("enumId2", "optionId2", "optionName2", 2, false);
        IEnumOption enumOption3 = new EnumOption("enumId3", "optionId3", "optionName3", 3, false);
        IEnumeration enumeration = mock(IEnumeration.class);
        when(enumeration.getAllOptions()).thenReturn(List.of(enumOption1, enumOption2, enumOption3));
        when(polarionService.getEnumeration(any(), any())).thenReturn(enumeration);

        Set<Option> enumSet = polarionService.getOptionsForEnum(new EnumType("enumId"), contextId);
        assertEquals(3, enumSet.size());
        assertTrue(enumSet.stream().anyMatch(option -> Objects.equals(option.getKey(), "optionId1") && Objects.equals(option.getName(), "optionName1")));
        assertTrue(enumSet.stream().anyMatch(option -> Objects.equals(option.getKey(), "optionId2") && Objects.equals(option.getName(), "optionName2")));
        assertTrue(enumSet.stream().anyMatch(option -> Objects.equals(option.getKey(), "optionId3") && Objects.equals(option.getName(), "optionName3")));
    }

    @Test
    void testGetEnumeration() {
        when(polarionService.getEnumeration(any(), any())).thenCallRealMethod();

        IDataService dataService = mock(IDataService.class);
        when(trackerService.getDataService()).thenReturn(dataService);
        IEnumeration enumeration = mock(IEnumeration.class);
        when(dataService.getEnumerationForEnumId(any(), any())).thenReturn(enumeration);

        assertSame(enumeration, polarionService.getEnumeration(mock(IEnumType.class), mock(IContextId.class)));
    }

    @Test
    void testGetPolarionProductName() {
        when(platformService.getPolarionProductName()).thenReturn(POLARION_PRODUCT_NAME);
        assertEquals(POLARION_PRODUCT_NAME, polarionService.getPolarionProductName());
    }

    @Test
    void testGetPolarionVersion() {
        when(platformService.getPolarionVersion()).thenReturn(POLARION_VERSION);
        assertEquals(POLARION_VERSION, polarionService.getPolarionVersion());
    }

    @NotNull
    private IProject mockProject(boolean existing) {
        IProject project = mock(IProject.class);
        when(projectService.getProject(anyString())).thenReturn(project);
        when(project.isUnresolvable()).thenReturn(!existing);

        return mockGetObjectRevision(project);
    }

    @NotNull
    private <T> T mockGetObjectRevision(@NotNull IPObject object) {
        IObjectId objectId = mock(IObjectId.class);
        lenient().when(object.getObjectId()).thenReturn(objectId);
        lenient().when(object.getDataSvc()).thenReturn(dataService);
        lenient().when(dataService.getVersionedInstance(any(IObjectId.class), any())).thenReturn(object);
        return (T) object;
    }

    @NotNull
    private IWorkItem mockWorkItem(@NotNull IProject project, boolean existing) {
        ITrackerProject trackerProject = mock(ITrackerProject.class);
        when(trackerService.getTrackerProject(project)).thenReturn(trackerProject);

        IWorkItem workItem = mock(IWorkItem.class);
        when(workItem.isUnresolvable()).thenReturn(!existing);
        when(trackerProject.getWorkItem(anyString())).thenReturn(workItem);

        return mockGetObjectRevision(workItem);
    }

    @NotNull
    private IModule mockModule(@NotNull IProject project, boolean existing) {
        IModule module = mock(IModule.class);
        when(module.isUnresolvable()).thenReturn(!existing);

        when(trackerService.getModuleManager()).thenReturn(moduleManager);
        when(moduleManager.getModule(eq(project), any())).thenReturn(module);

        return mockGetObjectRevision(module);
    }

    @NotNull
    private IBaselineCollection mockCollection(boolean existing) {
        IBaselineCollection collection = mock(IBaselineCollection.class);
        when(collection.isUnresolvable()).thenReturn(!existing);
        return mockGetObjectRevision(collection);
    }

}
