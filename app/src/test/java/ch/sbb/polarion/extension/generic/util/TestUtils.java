package ch.sbb.polarion.extension.generic.util;

import ch.sbb.polarion.extension.generic.service.PolarionService;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.platform.IPlatformService;
import com.polarion.platform.security.ISecurityService;
import com.polarion.platform.service.repository.IRepositoryService;
import lombok.experimental.UtilityClass;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@UtilityClass
public class TestUtils {

    public PolarionService mockPolarionService(ITrackerService trackerService, IProjectService projectService, ISecurityService securityService,
                                               IPlatformService platformService, IRepositoryService repositoryService) {
        PolarionService polarionService = mock(PolarionService.class, withSettings().useConstructor(
                trackerService == null ? mock(ITrackerService.class) : trackerService,
                projectService == null ? mock(IProjectService.class) : projectService,
                securityService == null ? mock(ISecurityService.class) : securityService,
                platformService == null ? mock(IPlatformService.class) : platformService,
                repositoryService == null ? mock(IRepositoryService.class) : repositoryService
        ));

        lenient().when(polarionService.getProject(anyString())).thenCallRealMethod();
        lenient().when(polarionService.getProject(anyString(), any())).thenCallRealMethod();
        lenient().when(polarionService.getTrackerProject(any())).thenCallRealMethod();
        lenient().when(polarionService.getWorkItem(anyString(), anyString())).thenCallRealMethod();
        lenient().when(polarionService.getWorkItem(anyString(), anyString(), any())).thenCallRealMethod();
        lenient().when(polarionService.getModule(any(), any())).thenCallRealMethod();
        lenient().when(polarionService.getModule(anyString(), anyString(), anyString())).thenCallRealMethod();
        lenient().when(polarionService.getModule(anyString(), anyString(), anyString(), any())).thenCallRealMethod();
        lenient().when(polarionService.getCollection(anyString(), anyString())).thenCallRealMethod();
        lenient().when(polarionService.getCollection(anyString(), anyString(), any())).thenCallRealMethod();

        lenient().when(polarionService.getResolvableObjectOrThrow(any(), any(), any())).thenCallRealMethod();
        lenient().when(polarionService.getObjectRevision(any(), any())).thenCallRealMethod();

        lenient().when(polarionService.getGeneralFields(any(), any())).thenCallRealMethod();
        lenient().when(polarionService.getCustomFields(any(), any(), any())).thenCallRealMethod();
        lenient().when(polarionService.getFieldValue(any(), any())).thenCallRealMethod();
        lenient().when(polarionService.getFieldValue(any(), any(), any())).thenCallRealMethod();
        lenient().when(polarionService.getOptionsForEnum(any(), any())).thenCallRealMethod();

        lenient().doCallRealMethod().when(polarionService).setFieldValue(any(),any(), any());
        lenient().doCallRealMethod().when(polarionService).setFieldValue(any(),any(), any(), any());

        lenient().when(polarionService.getPolarionProductName()).thenCallRealMethod();
        lenient().when(polarionService.getPolarionVersion()).thenCallRealMethod();

        mockSecureCalls(polarionService);
        return polarionService;
    }

    public MockedConstruction<PolarionService> mockPolarionServiceConstruction() {
        return mockConstruction(PolarionService.class,
                (mock, context) -> TestUtils.mockSecureCalls(mock));
    }

    public void mockSecureCalls(PolarionService polarionService) {
        ArgumentCaptor<Runnable> argument = ArgumentCaptor.forClass(Runnable.class);
        lenient().doAnswer(invocation -> {
            argument.getValue().run();
            return null;
        }).when(polarionService).callPrivileged(argument.capture());
    }
}
