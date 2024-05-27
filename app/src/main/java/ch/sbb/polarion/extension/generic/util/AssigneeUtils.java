package ch.sbb.polarion.extension.generic.util;

import ch.sbb.polarion.extension.generic.fields.converters.StringCSVToListConverter;
import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.persistence.model.IPObjectList;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@UtilityClass
@SuppressWarnings("unchecked")
public class AssigneeUtils {

    public static final String ASSIGNEE_FIELD_ID = "assignee";

    /**
     * Accepts wide range of params: string (also csv string), user or list of strings/users.
     * As a result work item will have exactly the same set of assignees as given in the param (any previously existing extra assignees will be removed).
     */
    public void setAssignees(IWorkItem workItem, Object value) {
        Set<IUser> usersToSet = new LinkedHashSet<>();
        parseAssigneeInput(workItem, value, usersToSet);
        setAssignees(workItem, usersToSet);
    }

    /**
     * Set assignee(s) to the work item.
     * As a result work item will have exactly the same set of assignees as given in the set (any previously existing extra assignees will be removed).
     */
    public void setAssignees(IWorkItem workItem, Set<IUser> usersToSet) {
        IPObjectList<IUser> currentAssignees = workItem.getAssignees();
        usersToSet.stream().filter(u -> !currentAssignees.contains(u)).forEach(workItem::addAssignee);
        workItem.getAssignees().removeAll(currentAssignees.stream().filter(a -> !usersToSet.contains(a)).toList());
    }

    @SuppressWarnings("rawtypes")
    private void parseAssigneeInput(IWorkItem workItem, Object value, Set<IUser> resultSet) {
        if (value == null) {
            return;
        }
        IPObjectList<IUser> allowedAssignees = workItem.getAllowedAssignees();
        if (value instanceof IUser iUser) {
            resultSet.add(iUser);
        } else if (value instanceof String assigneeString) {
            String[] assigneeEntries = assigneeString.split(Pattern.quote(StringCSVToListConverter.SEPARATOR));
            for (String assigneeEntry : assigneeEntries) {
                resultSet.add(allowedAssignees.stream().filter(a -> Arrays.asList(a.getId(), a.getLoginName(), a.getName()).contains(assigneeEntry))
                        .findFirst().orElseThrow(() -> new IllegalArgumentException("Cannot find allowed assignee '%s'".formatted(assigneeEntry))));
            }
        } else if (value instanceof List list) {
            for (Object listValue : list) {
                parseAssigneeInput(workItem, listValue, resultSet);
            }
        } else {
            throw new IllegalArgumentException("Unsupported assignee value '%s'".formatted(String.valueOf(value)));
        }
    }
}
