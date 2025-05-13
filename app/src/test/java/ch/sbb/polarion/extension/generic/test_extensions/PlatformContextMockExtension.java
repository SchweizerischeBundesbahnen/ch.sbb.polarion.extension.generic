package ch.sbb.polarion.extension.generic.test_extensions;

import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.tracker.ITestManagementService;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.platform.IPlatformService;
import com.polarion.platform.core.IPlatform;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.security.ILoginPolicy;
import com.polarion.platform.security.ISecurityService;
import com.polarion.platform.security.accesstoken.IUserAccessTokenService;
import com.polarion.platform.security.auth.UserAuthenticationProvider;
import com.polarion.platform.service.repository.IRepositoryService;
import com.polarion.portal.internal.server.navigation.TestManagementServiceAccessor;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Answers;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.*;

public class PlatformContextMockExtension implements BeforeEachCallback, AfterEachCallback {

    private MockedConstruction<TestManagementServiceAccessor> testManagementServiceAccessorMockedConstruction;
    private MockedStatic<PlatformContext> platformContextMockedStatic;

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        IPlatform platformMock = mock(IPlatform.class, Answers.RETURNS_DEEP_STUBS);

        ITrackerService trackerService = mock(ITrackerService.class, Answers.RETURNS_DEEP_STUBS);
        IProjectService projectService = mock(IProjectService.class, Answers.RETURNS_DEEP_STUBS);
        ISecurityService securityService = mock(ISecurityService.class, Answers.RETURNS_DEEP_STUBS);
        IPlatformService platformService = mock(IPlatformService.class, Answers.RETURNS_DEEP_STUBS);
        IRepositoryService repositoryService = mock(IRepositoryService.class, Answers.RETURNS_DEEP_STUBS);
        IDataService dataService = mock(IDataService.class, Answers.RETURNS_DEEP_STUBS);
        ILoginPolicy loginPolicy = mock(ILoginPolicy.class, Answers.RETURNS_DEEP_STUBS);
        ITestManagementService testManagementService = mock(ITestManagementService.class, Answers.RETURNS_DEEP_STUBS);
        UserAuthenticationProvider userAuthenticationProvider = mock(UserAuthenticationProvider.class, Answers.RETURNS_DEEP_STUBS);
        IUserAccessTokenService userAccessTokenService = mock(IUserAccessTokenService.class, Answers.RETURNS_DEEP_STUBS);

        lenient().when(platformMock.lookupService(ITrackerService.class)).thenReturn(trackerService);
        lenient().when(platformMock.lookupService(IProjectService.class)).thenReturn(projectService);
        lenient().when(platformMock.lookupService(ISecurityService.class)).thenReturn(securityService);
        lenient().when(platformMock.lookupService(IPlatformService.class)).thenReturn(platformService);
        lenient().when(platformMock.lookupService(IRepositoryService.class)).thenReturn(repositoryService);
        lenient().when(platformMock.lookupService(IDataService.class)).thenReturn(dataService);
        lenient().when(platformMock.lookupService(ILoginPolicy.class)).thenReturn(loginPolicy);
        lenient().when(platformMock.lookupService(UserAuthenticationProvider.class)).thenReturn(userAuthenticationProvider);
        lenient().when(platformMock.lookupService(IUserAccessTokenService.class)).thenReturn(userAccessTokenService);

        lenient().when(trackerService.getDataService()).thenReturn(dataService);

        platformContextMockedStatic = mockStatic(PlatformContext.class);
        platformContextMockedStatic.when(PlatformContext::getPlatform).thenReturn(platformMock);

        CustomExtensionMockInjector.inject(context, trackerService);
        CustomExtensionMockInjector.inject(context, projectService);
        CustomExtensionMockInjector.inject(context, securityService);
        CustomExtensionMockInjector.inject(context, platformService);
        CustomExtensionMockInjector.inject(context, repositoryService);
        CustomExtensionMockInjector.inject(context, dataService);
        CustomExtensionMockInjector.inject(context, loginPolicy);
        CustomExtensionMockInjector.inject(context, userAuthenticationProvider);
        CustomExtensionMockInjector.inject(context, userAccessTokenService);

        testManagementServiceAccessorMockedConstruction = mockConstruction(TestManagementServiceAccessor.class, (testManagementServiceAccessor, mockedContructionContext) -> {
            lenient().when(testManagementServiceAccessor.getTestingService()).thenReturn(testManagementService);
        });
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (testManagementServiceAccessorMockedConstruction != null) {
            testManagementServiceAccessorMockedConstruction.close();
        }
        if (platformContextMockedStatic != null) {
            platformContextMockedStatic.close();
        }
    }

}
