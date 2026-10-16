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
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.shared.api.model.baselinecollection.BaselineCollectionReference;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IStatusOpt;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.IWorkflowAction;
import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollection;
import com.polarion.core.util.StringUtils;
import com.polarion.core.util.logging.Logger;
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
import com.polarion.subterra.base.data.model.IRelationMapping;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Getter
@SuppressWarnings({"squid:S1200", "unused", "ClassCanBeRecord"}) // Ignore dependencies on other classes count limitation
public class PolarionService {

    private static final String STATUS_FIELD_ID = "status";
    private static final Logger logger = Logger.getLogger(PolarionService.class);

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
    public Set<FieldMetadata> getGeneralFields(@NotNull String proto, @NotNull IContextId contextId, @Nullable String optTypeId) {
        final IPrototype prototype = trackerService.getDataService().getPrototype(proto);
        return ((List<String>) prototype.getKeyNames()).stream()
                .map(keyName -> FieldMetadata.fromPrototype(prototype, keyName).setOptions(getOptionsForEnum(prototype.getKeyType(keyName), contextId, optTypeId)))
                .collect(Collectors.toSet());
    }

    public Set<FieldMetadata> getCustomFields(@NotNull String proto, @NotNull IContextId contextId, @Nullable String optTypeId) {
        final Collection<ICustomField> customFields = trackerService.getDataService().getCustomFieldsService().getCustomFields(proto, contextId, optTypeId);
        return customFields.stream()
                .map(customField -> FieldMetadata.fromCustomField(customField).setOptions(getOptionsForEnum(customField.getType(), contextId, optTypeId)))
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
        boolean multiValueField = fieldMetadata.getType() instanceof IListType;
        if (valueToSet == null) {
            if (fieldMetadata.isRequired()) {
                throw new IllegalArgumentException("Cannot set empty value to the required field");
            } else if (multiValueField) {
                valueToSet = new ArrayList<>(); //Multi-value fields expect empty list in case of empty value
            }
        }

        if (valueToSet instanceof List<?> valuesList) {
            if (multiValueField) {
                valueToSet = new CustomTypedList(ipObject, (IListType) fieldMetadata.getType(), fieldMetadata.isRequired(), valuesList);
            } else {
                throw new IllegalArgumentException("Cannot set multi-value into field '%s'".formatted(fieldId));
            }
        } else if (multiValueField) {
            throw new IllegalArgumentException("Cannot set single value to the multi-value field '%s'".formatted(fieldId));
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

    public Set<Option> getOptionsForEnum(final IType objectFieldType, final IContextId contextId, final String optTypeId) {
        IType fieldType = objectFieldType;
        if (fieldType instanceof ListType listType && listType.getItemType() instanceof EnumType enumType) { //in case of multiple enum field
            fieldType = enumType;
        }
        return fieldType instanceof EnumType enumType ? getEnumeration(enumType, contextId).getAvailableOptions(optTypeId).stream()
                .map(o -> new Option(EnumUtils.getEnumId(o), o.getName(), EnumUtils.getIconUrl(o))).collect(Collectors.toSet()) : null;
    }

    /**
     * Checks the enum values of the object against the dependencies of its custom fields
     * ("dependsOn" and "mapping" attributes in the custom fields configuration).
     * Polarion applies these dependencies only in its UI forms, its API accepts any combination.
     * Call this method after all fields are set, because the result depends on the value of the controlling field.
     * It also reports invalid values which the object held before the current change.
     *
     * @return the first violation found, or null if all values are allowed
     */
    @Nullable
    public String validateEnumDependencies(@NotNull IPObject object) {
        return findEnumDependencyViolation(object, null);
    }

    /**
     * Same as {@link #validateEnumDependencies(IPObject)}, but checks only the dependencies which include
     * at least one of the given fields, either as the dependent field or as the controlling one.
     *
     * @param fieldIds the fields to check, an empty collection checks nothing
     * @return the first violation found, or null if all values are allowed
     */
    @Nullable
    public String validateEnumDependencies(@NotNull IPObject object, @NotNull Collection<String> fieldIds) {
        return findEnumDependencyViolation(object, fieldIds);
    }

    @Nullable
    private String findEnumDependencyViolation(@NotNull IPObject object, @Nullable Collection<String> fieldIds) {
        for (String fieldId : object.getCustomFieldsList()) {
            ICustomField field = object.getCustomFieldPrototype(fieldId);
            String controllingFieldId = field.getDependsOn();
            if (StringUtils.isEmptyTrimmed(controllingFieldId)
                    || fieldIds != null && !fieldIds.contains(fieldId) && !fieldIds.contains(controllingFieldId)) {
                continue;
            }
            String error = validateEnumDependency(object, fieldId, field, controllingFieldId);
            if (error != null) {
                return error;
            }
        }
        return null;
    }

    @Nullable
    private String validateEnumDependency(@NotNull IPObject object, @NotNull String fieldId, @NotNull ICustomField field, @NotNull String controllingFieldId) {
        IRelationMapping mapping = field.getDependencyMapping();
        if (mapping == null) {
            return null;
        }
        List<IEnumOption> controllingOptions = toEnumOptions(getFieldValue(object, controllingFieldId));
        if (controllingOptions.isEmpty()) {
            return null; // Polarion UI offers all options while the controlling field is empty
        }
        Set<String> allowedIds = controllingOptions.stream()
                .flatMap(option -> mapping.getFilteredValues(option).stream())
                .filter(IEnumOption.class::isInstance)
                .map(option -> ((IEnumOption) option).getId())
                .collect(Collectors.toSet());
        return toEnumOptions(object.getCustomField(fieldId)).stream()
                .filter(option -> !allowedIds.contains(option.getId()))
                .findFirst()
                .map(option -> String.format("Can not set '%s' into '%s' field when '%s' is '%s'",
                        getOptionName(option), getFieldName(field, fieldId),
                        getFieldName(object.getCustomFieldPrototype(controllingFieldId), controllingFieldId),
                        controllingOptions.stream().map(this::getOptionName).collect(Collectors.joining(", "))))
                .orElse(null);
    }

    @NotNull
    private List<IEnumOption> toEnumOptions(@Nullable Object value) {
        if (value instanceof IEnumOption option) {
            return List.of(option);
        }
        if (value instanceof Collection<?> values) {
            return values.stream().filter(IEnumOption.class::isInstance).map(IEnumOption.class::cast).toList();
        }
        return List.of();
    }

    @NotNull
    private String getFieldName(@NotNull ICustomField field, @NotNull String fieldId) {
        return StringUtils.isEmptyTrimmed(field.getName()) ? fieldId : field.getName();
    }

    @NotNull
    private String getOptionName(@NotNull IEnumOption option) {
        return StringUtils.isEmptyTrimmed(option.getName()) ? option.getId() : option.getName();
    }

    /**
     * Performs a workflow action for a status which was written directly into the object.
     * Polarion runs workflow conditions and functions only in a workflow action, a direct write of the status skips them.
     * Call this method after all fields are set and before saving, so the action sees the new values of other fields.
     * Like Polarion's xUnit import, it performs the first available action which leads from the stored status to the new one.
     * The action can clear fields, which the workflow configures as cleared fields.
     *
     * @return null if the action succeeded or the status did not change, otherwise the reason why the action failed.
     * In this case the object keeps the new status, but workflow functions can have changed it, so do not save it.
     */
    @Nullable
    public String runWorkflowIfNeeded(@NotNull IPObject object) {
        if (!(object instanceof IWorkflowObject workflowObject) || !object.isPersisted()) {
            return null; // Polarion performs the initial action of a new object on its own
        }
        IStatusOpt newStatus = workflowObject.getStatus();
        IStatusOpt storedStatus = getObjectRevision(object, object.getLastRevision()) instanceof IWorkflowObject stored ? stored.getStatus() : null;
        if (Objects.equals(getStatusId(newStatus), getStatusId(storedStatus))) {
            return null;
        }
        // Polarion looks for actions starting from the current status
        workflowObject.setValue(STATUS_FIELD_ID, storedStatus);
        String error;
        try {
            error = performStatusChangeAction(workflowObject, storedStatus, newStatus);
        } catch (Exception e) {
            logger.error("Error performing workflow status change action", e);
            error = Optional.ofNullable(e.getMessage()).orElse(e.getClass().getSimpleName());
        } finally {
            workflowObject.setValue(STATUS_FIELD_ID, newStatus);
        }
        return error;
    }

    @Nullable
    private String performStatusChangeAction(@NotNull IWorkflowObject workflowObject, @Nullable IStatusOpt storedStatus, @Nullable IStatusOpt newStatus) {
        String newStatusId = getStatusId(newStatus);
        Optional<IWorkflowAction> action = Arrays.stream(workflowObject.getAvailableActions())
                .filter(availableAction -> availableAction.getTargetStatus().getId().equals(newStatusId))
                .findFirst();
        if (action.isPresent()) {
            workflowObject.performAction(action.get().getActionId());
            return null;
        }
        return workflowObject.getUnavailableActions().entrySet().stream()
                .filter(unavailableAction -> unavailableAction.getKey().getTargetStatus().getId().equals(newStatusId))
                .findFirst()
                .map(unavailableAction -> String.format("Workflow action '%s' to status '%s' is not available: %s",
                        getActionName(unavailableAction.getKey()), getStatusName(newStatus), unavailableAction.getValue()))
                .orElseGet(() -> String.format("No workflow action leads from status '%s' to status '%s'",
                        getStatusName(storedStatus), getStatusName(newStatus)));
    }

    @Nullable
    private String getStatusId(@Nullable IStatusOpt status) {
        return status == null ? null : status.getId();
    }

    @NotNull
    private String getStatusName(@Nullable IStatusOpt status) {
        return status == null ? "" : getOptionName(status);
    }

    @NotNull
    private String getActionName(@NotNull IWorkflowAction action) {
        return StringUtils.isEmptyTrimmed(action.getActionName()) ? action.getNativeActionId() : action.getActionName();
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

    /** Roles defined for the whole repository. */
    @NotNull
    public Collection<String> getGlobalRoles() {
        return securityService.getGlobalRoles();
    }

    /**
     * Roles defined for the project the scope points at. A scope naming no project - the global scope -
     * has none, which is a normal state and not an error.
     */
    @NotNull
    public Collection<String> getProjectRoles(@Nullable String scope) {
        String projectId = ScopeUtils.getProjectFromScope(scope == null ? "" : scope);
        if (projectId == null) {
            return Set.of();
        }
        return securityService.getContextRoles(getProject(projectId).getContextId());
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
