package ch.sbb.polarion.extension.generic.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.polarion.core.util.StringUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A setting that stores "who is allowed to do this" as two lists of role names - the shape several
 * extensions had each written for themselves.
 * <p>
 * The stored format is unchanged and is keyed by entry name ({@code globalRoles} / {@code projectRoles}),
 * not by class, so settings written by an extension's own copy of this model deserialize here as they
 * are.
 * <p>
 * The setting's <em>name</em> stays with the extension: subclass {@link GenericNamedSettings} with
 * whatever feature name it stores under. One extension may well have several - api-extender keeps
 * project custom fields and global records apart this way.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class AuthorizationModel extends SettingsModel {

    public static final String GLOBAL_ROLES = "globalRoles";
    public static final String PROJECT_ROLES = "projectRoles";

    protected List<String> globalRoles;
    protected List<String> projectRoles;

    public void setGlobalRoles(String... roles) {
        globalRoles = List.of(roles);
    }

    public void setProjectRoles(String... roles) {
        projectRoles = List.of(roles);
    }

    @Override
    protected String serializeModelData() {
        return serializeEntry(GLOBAL_ROLES, serializeRoles(globalRoles)) +
                serializeEntry(PROJECT_ROLES, serializeRoles(projectRoles));
    }

    @Override
    protected void deserializeModelData(String serializedString) {
        globalRoles = deserializeRoles(GLOBAL_ROLES, serializedString);
        projectRoles = deserializeRoles(PROJECT_ROLES, serializedString);
    }

    @NotNull
    protected String serializeRoles(@Nullable List<String> roles) {
        return roles == null ? "" : String.join(",", roles);
    }

    @NotNull
    protected List<String> deserializeRoles(@NotNull String what, @NotNull String serializedString) {
        final String roles = deserializeEntry(what, serializedString);
        // Trim first, then drop the blanks: filtering before trimming let an entry of spaces through
        // as an empty string, which is not a role and can never match one.
        return Arrays.stream(roles.split(",")).map(String::trim).filter(s -> !StringUtils.isEmpty(s)).toList();
    }

    /** Every granted role, global and project, in one list - what a permission check usually wants. */
    @JsonIgnore
    public List<String> getAllRoles() {
        List<String> roles = new ArrayList<>();
        roles.addAll(getGlobalRoles());
        roles.addAll(getProjectRoles());
        return roles;
    }
}
