package ch.sbb.polarion.extension.generic.service;

import ch.sbb.polarion.extension.generic.exception.ObjectNotFoundException;
import ch.sbb.polarion.extension.generic.fields.ConverterContext;
import ch.sbb.polarion.extension.generic.fields.IConverter;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import ch.sbb.polarion.extension.generic.fields.model.Option;
import ch.sbb.polarion.extension.generic.util.AssigneeUtils;
import ch.sbb.polarion.extension.generic.util.EnumUtils;
import ch.sbb.polarion.extension.generic.util.ObjectUtils;
import ch.sbb.polarion.extension.generic.util.RequestContextUtil;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.shared.api.model.baselinecollection.BaselineCollectionReference;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollection;
import com.polarion.core.util.StringUtils;
import com.polarion.platform.IPlatformService;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.IEnumeration;
import com.polarion.platform.persistence.model.IPObject;
import com.polarion.platform.persistence.model.IPrototype;
import com.polarion.platform.persistence.spi.CustomTypedList;
import com.polarion.platform.security.ISecurityService;
import com.polarion.platform.service.repository.IRepositoryConnection;
import com.polarion.platform.service.repository.IRepositoryReadOnlyConnection;
import com.polarion.platform.service.repository.IRepositoryService;
import com.polarion.subterra.base.data.identification.IContextId;
import com.polarion.subterra.base.data.model.ICustomField;
import com.polarion.subterra.base.data.model.IEnumType;
import com.polarion.subterra.base.data.model.IListType;
import com.polarion.subterra.base.data.model.IType;
import com.polarion.subterra.base.data.model.internal.EnumType;
import com.polarion.subterra.base.data.model.internal.ListType;
import com.polarion.subterra.base.location.ILocation;
import com.polarion.subterra.base.location.Location;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Getter
@SuppressWarnings({"squid:S1200", "unused"}) // Ignore dependencies on other classes count limitation
public class PolarionService {

    protected final ITrackerService trackerService;
    protected final IProjectService projectService;
    protected final ISecurityService securityService;
    protected final IPlatformService platformService;
    protected final IRepositoryService repositoryService;

    public PolarionService() {
        trackerService = PlatformContext.getPlatform().lookupService(ITrackerService.class);
        projectService = PlatformContext.getPlatform().lookupService(IProjectService.class);
        securityService = PlatformContext.getPlatform().lookupService(ISecurityService.class);
        platformService = PlatformContext.getPlatform().lookupService(IPlatformService.class);
        repositoryService = PlatformContext.getPlatform().lookupService(IRepositoryService.class);
    }

    public PolarionService(@NotNull ITrackerService trackerService,
                           @NotNull IProjectService projectService,
                           @NotNull ISecurityService securityService,
                           @NotNull IPlatformService platformService,
                           @NotNull IRepositoryService repositoryService) {
        this.trackerService = trackerService;
        this.projectService = projectService;
        this.securityService = securityService;
        this.platformService = platformService;
        this.repositoryService = repositoryService;
    }

    @NotNull
    public IProject getProject(@NotNull String projectId) {
        return getProject(projectId, null);
    }

    @NotNull
    public IProject getProject(@NotNull String projectId, @Nullable String revision) {
        if (StringUtils.isEmptyTrimmed(projectId)) {
            throw new IllegalArgumentException("Parameter 'projectId' should be provided");
        }

        //Note: it seems that there is no way to get project for already deleted project (unlike workitems/modules & collections)
        //so method below will throw exception in this case
        return getResolvableObjectOrThrow(projectService.getProject(projectId), revision, String.format("Project '%s'%s not found", projectId, getRevisionMessagePart(revision)));
    }

    @NotNull
    public ITrackerProject getTrackerProject(@NotNull String projectId) {
        IProject project = getProject(projectId);
        return trackerService.getTrackerProject(project);
    }

    @NotNull
    public IWorkItem getWorkItem(@NotNull String projectId, @NotNull String workItemId) {
        return getWorkItem(projectId, workItemId, null);
    }

    @NotNull
    public IWorkItem getWorkItem(@NotNull String projectId, @NotNull String workItemId, @Nullable String revision) {
        ITrackerProject trackerProject = getTrackerProject(projectId);
        if (StringUtils.isEmptyTrimmed(workItemId)) {
            throw new IllegalArgumentException("Parameter 'workItemId' should be provided");
        }

        return getResolvableObjectOrThrow(trackerProject.getWorkItem(workItemId), revision, String.format("WorkItem '%s'%s not found in project '%s'", workItemId, getRevisionMessagePart(revision), projectId));
    }

    @NotNull
    public IModule getModule(@NotNull String projectId, @NotNull String spaceId, @NotNull String documentName) {
        return getModule(projectId, spaceId, documentName, null);
    }

    @NotNull
    public IModule getModule(@NotNull String projectId, @NotNull String spaceId, @NotNull String documentName, @Nullable String revision) {
        IProject project = getProject(projectId);
        if (StringUtils.isEmptyTrimmed(spaceId)) {
            throw new IllegalArgumentException("Parameter 'spaceId' should be provided");
        }
        if (StringUtils.isEmptyTrimmed(documentName)) {
            throw new IllegalArgumentException("Parameter 'documentName' should be provided");
        }

        ILocation location = Location.getLocation(spaceId + "/" + documentName);
        return getResolvableObjectOrThrow(getModule(project, location), revision, String.format("Document '%s'%s not found in space '%s' of project '%s'", documentName, getRevisionMessagePart(revision), spaceId, projectId));
    }

    @NotNull
    public IModule getModule(@NotNull IProject project, @NotNull ILocation location) {
        return trackerService.getModuleManager().getModule(project, location);
    }

    @NotNull
    public IBaselineCollection getCollection(@NotNull String projectId, @NotNull String collectionId) {
        return getCollection(projectId, collectionId, null);
    }

    @NotNull
    public IBaselineCollection getCollection(@NotNull String projectId, @NotNull String collectionId, @Nullable String revision) {
        IProject project = getProject(projectId);
        if (StringUtils.isEmptyTrimmed(collectionId)) {
            throw new IllegalArgumentException("Parameter 'collectionId' should be provided");
        }

        IBaselineCollection collection = ObjectUtils.requireNotNull(
                TransactionalExecutor.executeSafelyInReadOnlyTransaction(
                        transaction -> new BaselineCollectionReference(project.getId(), collectionId)
                                .get(transaction)
                                .getOldApi()
                )
        );
        return getResolvableObjectOrThrow(collection, revision, String.format("Collection with id '%s'%s not found in project '%s'", collectionId, getRevisionMessagePart(revision), projectId));
    }

    public <T extends IPObject> T getResolvableObjectOrThrow(@NotNull IPObject object, @Nullable String revision, @NotNull String notFoundMessage) {
        return Optional.<T>of(getObjectRevision(object, revision))
                .filter(w -> !w.isUnresolvable())
                .orElseThrow(() -> new ObjectNotFoundException(notFoundMessage));
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public <T extends IPObject> T getObjectRevision(@NotNull IPObject object, @Nullable String revision) {
        if (revision != null) {
            IDataService dataSvc = object.getDataSvc();
            return (T) dataSvc.getVersionedInstance(object.getObjectId(), revision);
        } else {
            return (T) object;
        }
    }

    @SuppressWarnings({"unchecked"})
    public Set<FieldMetadata> getGeneralFields(@NotNull String proto, @NotNull IContextId contextId) {
        final IPrototype prototype = trackerService.getDataService().getPrototype(proto);
        return ((List<String>) prototype.getKeyNames()).stream()
                .map(keyName -> FieldMetadata.fromPrototype(prototype, keyName).setOptions(getOptionsForEnum(prototype.getKeyType(keyName), contextId)))
                .collect(Collectors.toSet());
    }

    public Set<FieldMetadata> getCustomFields(@NotNull String proto, @NotNull IContextId contextId, String optTypeId) {
        final Collection<ICustomField> customFields = trackerService.getDataService().getCustomFieldsService().getCustomFields(proto, contextId, optTypeId);
        return customFields.stream()
                .map(customField -> FieldMetadata.fromCustomField(customField).setOptions(getOptionsForEnum(customField.getType(), contextId)))
                .collect(Collectors.toSet());
    }

    public void setFieldValue(@NotNull IPObject ipObject, @NotNull String fieldId, Object value) {
        setFieldValue(ipObject, fieldId, value, null);
    }

    @SuppressWarnings("unchecked")
    public void setFieldValue(@NotNull IPObject ipObject, @NotNull String fieldId, Object value, Map<String, Map<String, String>> enumsMapping) {
        if (ipObject instanceof IWorkItem workItem && AssigneeUtils.ASSIGNEE_FIELD_ID.equals(fieldId)) {
            AssigneeUtils.setAssignees(workItem, value);
            return;
        }
        FieldMetadata fieldMetadata = ipObject.getPrototype().isKeyDefined(fieldId) ?
                FieldMetadata.fromPrototype(ipObject.getPrototype(), fieldId) : FieldMetadata.fromCustomField(ipObject.getCustomFieldPrototype(fieldId));
        Object valueToSet = value == null ? null : Optional.ofNullable(IConverter.getSuitableConverter(value, fieldMetadata.getType()))
                .map(converter -> converter.convert(value, ConverterContext.builder().contextId(ipObject.getContextId()).enumsMapping(enumsMapping).build(), fieldMetadata)).orElse(value);
        if (valueToSet == null) {
            if (fieldMetadata.isRequired()) {
                throw new IllegalArgumentException("Cannot set empty value to the required field");
            } else if (fieldMetadata.getType() instanceof IListType) {
                valueToSet = new ArrayList<>(); //Multi-value fields expect empty list in case of empty value
            }
        }
        if (valueToSet instanceof List<?> valuesList) {
            valueToSet = new CustomTypedList(ipObject, (IListType) fieldMetadata.getType(), fieldMetadata.isRequired(), valuesList);
        }
        if (fieldMetadata.isCustom()) {
            ipObject.setCustomField(fieldId, valueToSet);
        } else {
            ipObject.setValue(fieldId, valueToSet);
        }
    }

    public Object getFieldValue(@NotNull IPObject ipObject, @NotNull String fieldId) {
        return ipObject.getPrototype().isKeyDefined(fieldId) ? ipObject.getValue(fieldId) : ipObject.getCustomField(fieldId);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object getFieldValue(@NotNull IPObject ipObject, @NotNull String fieldId, @NotNull Class preferredReturnType) {
        Object value = getFieldValue(ipObject, fieldId);
        if (value == null) {
            return null;
        }
        FieldMetadata fieldMetadata = ipObject.getPrototype().isKeyDefined(fieldId) ?
                FieldMetadata.fromPrototype(ipObject.getPrototype(), fieldId) : FieldMetadata.fromCustomField(ipObject.getCustomFieldPrototype(fieldId));
        return Optional.ofNullable(IConverter.getSuitableConverter(preferredReturnType, fieldMetadata.getType()))
                .map(converter -> converter.convertBack(value, ConverterContext.builder().contextId(ipObject.getContextId()).preferredReturnType(preferredReturnType).build(), fieldMetadata)).orElse(value);
    }

    @SuppressWarnings("unchecked")
    public IEnumeration<IEnumOption> getEnumeration(@NotNull IEnumType enumType, @NotNull IContextId contextId) {
        return trackerService.getDataService().getEnumerationForEnumId(enumType, contextId);
    }

    public Set<Option> getOptionsForEnum(final IType objectFieldType, final IContextId contextId) {
        IType fieldType = objectFieldType;
        if (fieldType instanceof ListType listType && listType.getItemType() instanceof EnumType enumType) { //in case of multiple enum field
            fieldType = enumType;
        }
        return fieldType instanceof EnumType enumType ? getEnumeration(enumType, contextId).getAllOptions().stream()
                .map(o -> new Option(EnumUtils.getEnumId(o), o.getName())).collect(Collectors.toSet()) : null;
    }

    @SneakyThrows
    public <T> T callPrivileged(Callable<T> callable) {
        return securityService.doAsUser(RequestContextUtil.getUserSubject(), (PrivilegedExceptionAction<T>) callable::call);
    }

    @SneakyThrows
    public void callPrivileged(Runnable runnable) {
        securityService.doAsUser(RequestContextUtil.getUserSubject(), (PrivilegedAction<Object>) () -> {
                    runnable.run();
                    return null;
                }
        );
    }

    public String getPolarionProductName() {
        return platformService.getPolarionProductName();
    }

    public String getPolarionVersion() {
        return platformService.getPolarionVersion();
    }

    @NotNull
    public IRepositoryConnection getConnection(@NotNull ILocation location) {
        return repositoryService.getConnection(location);
    }

    @NotNull
    public IRepositoryReadOnlyConnection getReadOnlyConnection(@NotNull ILocation location) {
        return repositoryService.getReadOnlyConnection(location);
    }

    private String getRevisionMessagePart(String revision) {
        return StringUtils.isEmpty(revision) ? "" : " (rev. %s)".formatted(revision);
    }
}
