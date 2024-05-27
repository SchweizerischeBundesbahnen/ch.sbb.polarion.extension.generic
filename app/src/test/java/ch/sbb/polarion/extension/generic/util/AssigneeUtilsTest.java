package ch.sbb.polarion.extension.generic.util;

import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.persistence.model.IPObjectList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "UnusedReturnValue"})
class AssigneeUtilsTest {

    @Test
    void testSetAssignees() {

        // not allowed user
        IWorkItem workItemWithoutAllowedUsers = mockData(List.of(), List.of());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> AssigneeUtils.setAssignees(workItemWithoutAllowedUsers, "testUser"));
        assertEquals("Cannot find allowed assignee 'testUser'", exception.getMessage());

        // set to work items without assignee
        IWorkItem workItem = mockData(List.of(user("testUser")), List.of());
        AssigneeUtils.setAssignees(workItem, "testUser");

        ArgumentCaptor<IUser> userCaptor = ArgumentCaptor.forClass(IUser.class);
        verify(workItem, times(1)).addAssignee(userCaptor.capture());
        assertEquals("testUser", userCaptor.getValue().getId());

        // user already set
        workItem = mockData(List.of(user("testUser")), List.of(user("testUser")));
        AssigneeUtils.setAssignees(workItem, "testUser");

        verify(workItem, times(0)).addAssignee(any());

        // use IUser object + remove several existing
        workItem = mockData(List.of(user("testUser")), List.of(user("testUser2"), user("testUser3"), user("testUser4")));
        assertEquals(3, workItem.getAssignees().size());
        AssigneeUtils.setAssignees(workItem, user("testUser"));

        verify(workItem, times(1)).addAssignee(any());
        assertTrue(workItem.getAssignees().isEmpty());

        // set strings list
        workItem = mockData(List.of(user("testUser1"), user("testUser2"), user("testUser3")), List.of());
        List<String> userIds = List.of("testUser1", "testUser2", "testUser3");
        AssigneeUtils.setAssignees(workItem, userIds);

        userCaptor = ArgumentCaptor.forClass(IUser.class);
        verify(workItem, times(3)).addAssignee(userCaptor.capture());
        assertTrue(userCaptor.getAllValues().stream().map(IUser::getId).toList().containsAll(userIds));

        // set IUser objects list
        workItem = mockData(List.of(user("testUser1"), user("testUser2"), user("testUser3")), List.of());
        List<IUser> users = List.of(user("testUser1"), user("testUser2"), user("testUser3"));
        AssigneeUtils.setAssignees(workItem, users);

        userCaptor = ArgumentCaptor.forClass(IUser.class);
        verify(workItem, times(3)).addAssignee(userCaptor.capture());
        assertTrue(userCaptor.getAllValues().containsAll(users));

        // unsupported type
        IWorkItem effectivelyFinalWorkItem = mockData(List.of(user("testUser1")), List.of());
        Integer inputObject = 42;
        assertEquals("Unsupported assignee value '42'",
                assertThrows(IllegalArgumentException.class, () ->
                        AssigneeUtils.setAssignees(effectivelyFinalWorkItem, inputObject)).getMessage());
    }

    private IWorkItem mockData(List<IUser> allowedUsers, List<IUser> existingUsers) {
        IWorkItem workItem = mock(IWorkItem.class);

        IPObjectList allowedList = new PObjectListStub(allowedUsers);
        lenient().when(workItem.getAllowedAssignees()).thenReturn(allowedList);

        IPObjectList existingList = spy(new PObjectListStub(existingUsers));
        lenient().when(existingList.contains(any(IUser.class))).thenAnswer((Answer<Boolean>) invocation ->
                existingUsers.stream().anyMatch(u -> Objects.equals(u.getId(), ((IUser) invocation.getArguments()[0]).getId())));
        lenient().when(workItem.getAssignees()).thenReturn(existingList);
        return workItem;
    }

    IUser user(String name) {
        IUser user = mock(IUser.class);
        lenient().when(user.getId()).thenReturn(name);
        return user;
    }

}