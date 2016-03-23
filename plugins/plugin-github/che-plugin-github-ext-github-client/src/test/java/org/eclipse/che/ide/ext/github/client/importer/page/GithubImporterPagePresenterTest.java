/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.github.client.importer.page;

import com.google.gwt.core.client.JsArrayMixed;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.api.project.shared.dto.ProjectImporterDescriptor;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.user.gwt.client.UserServiceClient;
import org.eclipse.che.api.user.shared.dto.ProfileDescriptor;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.SourceStorageDto;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentUser;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.oauth.OAuth2Authenticator;
import org.eclipse.che.ide.api.oauth.OAuth2AuthenticatorRegistry;
import org.eclipse.che.ide.api.wizard.Wizard;
import org.eclipse.che.ide.commons.exception.UnauthorizedException;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.github.client.GitHubClientService;
import org.eclipse.che.ide.ext.github.client.GitHubLocalizationConstant;
import org.eclipse.che.ide.ext.github.client.load.ProjectData;
import org.eclipse.che.ide.ext.github.shared.GitHubRepository;
import org.eclipse.che.ide.ext.github.shared.GitHubUser;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.security.oauth.OAuthStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testing {@link GithubImporterPagePresenter} functionality.
 *
 * @author Roman Nikitenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class GithubImporterPagePresenterTest {

    @Captor
    private ArgumentCaptor<AsyncCallback<OAuthStatus>> asyncCallbackCaptor;

    @Captor
    private ArgumentCaptor<AsyncRequestCallback<Map<String, List<GitHubRepository>>>> asyncRequestCallbackRepoListCaptor;

    @Mock
    private Wizard.UpdateDelegate           updateDelegate;
    @Mock
    private DtoFactory                      dtoFactory;
    @Mock
    private GithubImporterPageView          view;
    @Mock
    private UserServiceClient               userServiceClient;
    @Mock
    private GitHubClientService             gitHubClientService;
    @Mock
    private DtoUnmarshallerFactory          dtoUnmarshallerFactory;
    @Mock
    private NotificationManager             notificationManager;
    @Mock
    private GitHubLocalizationConstant      locale;
    @Mock
    private ProjectConfigDto                dataObject;
    @Mock
    private SourceStorageDto                source;
    @Mock
    private Map<String, String>             parameters;
    @Mock
    private Promise<GitHubUser>             gitHubUserPromise;
    @Mock
    private Promise<List<GitHubUser>>       gitHubOrgsPromise;
    @Mock
    private Promise<List<GitHubRepository>> gitHubReposPromise;
    @Mock
    private JsArrayMixed                    jsArrayMixed;
    @Mock
    private GitHubUser                      gitHubUser;
    @Mock
    private PromiseError                    promiseError;
    @Mock
    private Response                        response;
    @Mock
    private OAuth2Authenticator             gitHubAuthenticator;
    @Mock
    private OAuth2AuthenticatorRegistry     gitHubAuthenticatorRegistry;
    @Mock
    private AppContext                      appContext;

    private GithubImporterPagePresenter presenter;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(dataObject.getSource()).thenReturn(source);
        Mockito.doReturn("AccountName").when(gitHubUser).getName();
        Mockito.doReturn("AccountLogin").when(gitHubUser).getLogin();
        Mockito.when(gitHubAuthenticatorRegistry.getAuthenticator(Matchers.eq("github"))).thenReturn(gitHubAuthenticator);
        presenter = Mockito
                .spy(new GithubImporterPagePresenter(view, gitHubAuthenticatorRegistry, gitHubClientService, dtoFactory, "", appContext,
                                                     locale));
        Mockito.doReturn(Collections.singletonList(gitHubUser)).when(presenter).toOrgList(Matchers.any(JsArrayMixed.class));
        Mockito.doReturn(Collections.emptyList()).when(presenter).toRepoList(Matchers.any(JsArrayMixed.class));
        presenter.setUpdateDelegate(updateDelegate);
        presenter.init(dataObject);
    }

    @Test
    public void delegateShouldBeSet() throws Exception {
        Mockito.verify(view).setDelegate(Matchers.any(GithubImporterPagePresenter.class));
    }

    @Test
    public void testGo() throws Exception {
        String importerDescription = "description";
        AcceptsOneWidget container = Mockito.mock(AcceptsOneWidget.class);
        ProjectImporterDescriptor projectImporter = Mockito.mock(ProjectImporterDescriptor.class);
        //when(wizardContext.getData(ImportProjectWizard.PROJECT_IMPORTER)).thenReturn(projectImporter);
        Mockito.when(projectImporter.getDescription()).thenReturn(importerDescription);

        presenter.go(container);

        Mockito.verify(view).setInputsEnableState(Matchers.eq(true));
        Mockito.verify(container).setWidget(Matchers.eq(view));
        Mockito.verify(view).focusInUrlInput();
    }

    @Test
    public void onLoadRepoClickedWhenGetUserReposIsSuccessful() throws Exception {
        Mockito.doAnswer(new Answer() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                presenter.onSuccessRequest(jsArrayMixed);
                return null;
            }
        }).when(presenter).doRequest(Matchers.any(Promise.class), Matchers.any(Promise.class), Matchers.any(Promise.class));
        Mockito.when(view.getAccountName()).thenReturn("AccountName");

        presenter.onLoadRepoClicked();

        Mockito.verify(gitHubClientService).getRepositoriesList();
        Mockito.verify(gitHubClientService).getUserInfo();
        Mockito.verify(gitHubClientService).getOrganizations();

        Mockito.verify(notificationManager, Mockito.never()).notify(Matchers.anyString(), Matchers.any(ProjectConfigDto.class));
        Mockito.verify(view).setLoaderVisibility(Matchers.eq(true));
        Mockito.verify(view).setInputsEnableState(Matchers.eq(false));
        Mockito.verify(view).setLoaderVisibility(Matchers.eq(false));
        Mockito.verify(view).setInputsEnableState(Matchers.eq(true));
        Mockito.verify(view).setAccountNames(Matchers.<Set>anyObject());
        Mockito.verify(view, Mockito.times(2)).showGithubPanel();
        Mockito.verify(view).setRepositories(Matchers.<List<ProjectData>>anyObject());
        Mockito.verify(view).reset();
    }

    @Test
    public void onLoadRepoClickedWhenGetUserReposIsFailed() throws Exception {
        Mockito.doAnswer(new Answer() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                presenter.onFailRequest(promiseError);
                return null;
            }
        }).when(presenter).doRequest(Matchers.any(Promise.class), Matchers.any(Promise.class), Matchers.any(Promise.class));

        presenter.onLoadRepoClicked();

        Mockito.verify(gitHubClientService).getRepositoriesList();
        Mockito.verify(gitHubClientService).getUserInfo();
        Mockito.verify(gitHubClientService).getOrganizations();

        Mockito.verify(view).setLoaderVisibility(Matchers.eq(true));
        Mockito.verify(view).setInputsEnableState(Matchers.eq(false));
        Mockito.verify(view).setLoaderVisibility(Matchers.eq(false));
        Mockito.verify(view).setInputsEnableState(Matchers.eq(true));
        Mockito.verify(view, Mockito.never()).setAccountNames((Set<String>)Matchers.anyObject());
        Mockito.verify(view, Mockito.never()).showGithubPanel();
        Mockito.verify(view, Mockito.never()).setRepositories(Matchers.<List<ProjectData>>anyObject());
    }

    @Test
    public void onRepositorySelectedTest() {
        ProjectData projectData = new ProjectData("name", "description", "type", new ArrayList<String>(), "repoUrl", "readOnlyUrl");

        presenter.onRepositorySelected(projectData);

        Mockito.verify(dataObject).setName(Matchers.eq("name"));
        Mockito.verify(dataObject).setDescription(Matchers.eq("description"));
        Mockito.verify(source).setLocation(Matchers.eq("repoUrl"));
        Mockito.verify(view).setProjectName(Matchers.anyString());
        Mockito.verify(view).setProjectDescription(Matchers.anyString());
        Mockito.verify(view).setProjectUrl(Matchers.anyString());
        Mockito.verify(updateDelegate).updateControls();
    }

    @Test
    public void projectUrlStartWithWhiteSpaceEnteredTest() {
        String incorrectUrl = " https://github.com/codenvy/ide.git";
        Mockito.when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(incorrectUrl);

        Mockito.verify(view).markURLInvalid();
        Mockito.verify(view).setURLErrorMessage(eq(locale.importProjectMessageStartWithWhiteSpace()));
        Mockito.verify(source).setLocation(Matchers.eq(incorrectUrl));
        Mockito.verify(view).setProjectName(Matchers.anyString());
        Mockito.verify(updateDelegate).updateControls();
    }

    @Test
    public void testUrlMatchScpLikeSyntax() {
        // test for url with an alternative scp-like syntax: [user@]host.xz:path/to/repo.git/
        String correctUrl = "host.xz:path/to/repo.git";
        Mockito.when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(correctUrl);

        verifyInvocationsForCorrectUrl(correctUrl);
    }

    @Test
    public void testUrlWithoutUsername() {
        String correctUrl = "git@hostname.com:projectName.git";
        Mockito.when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(correctUrl);

        verifyInvocationsForCorrectUrl(correctUrl);
    }

    @Test
    public void testSshUriWithHostBetweenDoubleSlashAndSlash() {
        //Check for type uri which start with ssh:// and has host between // and /
        String correctUrl = "ssh://host.com/some/path";
        Mockito.when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(correctUrl);

        verifyInvocationsForCorrectUrl(correctUrl);
    }

    @Test
    public void testSshUriWithHostBetweenDoubleSlashAndColon() {
        //Check for type uri with host between // and :
        String correctUrl = "ssh://host.com:port/some/path";
        Mockito.when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(correctUrl);

        verifyInvocationsForCorrectUrl(correctUrl);
    }

    @Test
    public void testGitUriWithHostBetweenDoubleSlashAndSlash() {
        //Check for type uri which start with git:// and has host between // and /
        String correctUrl = "git://host.com/user/repo";
        Mockito.when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(correctUrl);

        verifyInvocationsForCorrectUrl(correctUrl);
    }

    @Test
    public void testSshUriWithHostBetweenAtAndColon() {
        //Check for type uri with host between @ and :
        String correctUrl = "user@host.com:login/repo";
        Mockito.when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(correctUrl);

        verifyInvocationsForCorrectUrl(correctUrl);
    }

    @Test
    public void testSshUriWithHostBetweenAtAndSlash() {
        //Check for type uri with host between @ and /
        String correctUrl = "ssh://user@host.com/some/path";
        Mockito.when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(correctUrl);

        verifyInvocationsForCorrectUrl(correctUrl);
    }

    @Test
    public void projectUrlWithIncorrectProtocolEnteredTest() {
        String correctUrl = "htps://github.com/codenvy/ide.git";
        Mockito.when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(correctUrl);

        Mockito.verify(view).markURLInvalid();
        Mockito.verify(view).setURLErrorMessage(eq(locale.importProjectMessageProtocolIncorrect()));
        Mockito.verify(source).setLocation(Matchers.eq(correctUrl));
        Mockito.verify(view).setProjectName(Matchers.anyString());
        Mockito.verify(updateDelegate).updateControls();
    }

    @Test
    public void correctProjectNameEnteredTest() {
        String correctName = "angularjs";
        Mockito.when(view.getProjectName()).thenReturn(correctName);

        presenter.projectNameChanged(correctName);

        Mockito.verify(dataObject).setName(Matchers.eq(correctName));
        Mockito.verify(view).markNameValid();
        Mockito.verify(view, Mockito.never()).markNameInvalid();
        Mockito.verify(updateDelegate).updateControls();
    }

    @Test
    public void correctProjectNameWithPointEnteredTest() {
        String correctName = "Test.project..ForCodenvy";
        Mockito.when(view.getProjectName()).thenReturn(correctName);

        presenter.projectNameChanged(correctName);

        Mockito.verify(dataObject).setName(Matchers.eq(correctName));
        Mockito.verify(view).markNameValid();
        Mockito.verify(view, Mockito.never()).markNameInvalid();
        Mockito.verify(updateDelegate).updateControls();
    }

    @Test
    public void emptyProjectNameEnteredTest() {
        String emptyName = "";
        Mockito.when(view.getProjectName()).thenReturn(emptyName);

        presenter.projectNameChanged(emptyName);

        Mockito.verify(dataObject).setName(Matchers.eq(emptyName));
        Mockito.verify(updateDelegate).updateControls();
    }

    @Test
    public void incorrectProjectNameEnteredTest() {
        String incorrectName = "angularjs+";
        Mockito.when(view.getProjectName()).thenReturn(incorrectName);

        presenter.projectNameChanged(incorrectName);

        Mockito.verify(dataObject).setName(Matchers.eq(incorrectName));
        Mockito.verify(view).markNameInvalid();
        Mockito.verify(updateDelegate).updateControls();
    }

    @Test
    public void projectDescriptionChangedTest() {
        String description = "description";
        presenter.projectDescriptionChanged(description);

        Mockito.verify(dataObject).setDescription(Matchers.eq(description));
    }

    @Test
    public void onLoadRepoClickedWhenAuthorizeIsFailed() throws Exception {
        String userId = "userId";
        CurrentUser user = Mockito.mock(CurrentUser.class);
        ProfileDescriptor profile = Mockito.mock(ProfileDescriptor.class);

        Mockito.when(appContext.getCurrentUser()).thenReturn(user);
        Mockito.when(user.getProfile()).thenReturn(profile);
        Mockito.when(profile.getId()).thenReturn(userId);


        final Throwable exception = Mockito.mock(UnauthorizedException.class);
        Mockito.doAnswer(new Answer() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                presenter.onFailRequest(promiseError);
                return null;
            }
        }).when(presenter).doRequest(Matchers.any(Promise.class), Matchers.any(Promise.class), Matchers.any(Promise.class));
        Mockito.doReturn(exception).when(promiseError).getCause();

        presenter.onLoadRepoClicked();

        Mockito.verify(gitHubClientService).getRepositoriesList();
        Mockito.verify(gitHubClientService).getUserInfo();
        Mockito.verify(gitHubClientService).getOrganizations();

        Mockito.verify(gitHubAuthenticator).authenticate(Matchers.anyString(), asyncCallbackCaptor.capture());
        AsyncCallback<OAuthStatus> asyncCallback = asyncCallbackCaptor.getValue();
        asyncCallback.onFailure(exception);

        Mockito.verify(view, Mockito.times(2)).setLoaderVisibility(Matchers.eq(true));
        Mockito.verify(view, Mockito.times(2)).setInputsEnableState(Matchers.eq(false));
        Mockito.verify(view, Mockito.times(2)).setInputsEnableState(Matchers.eq(true));
        Mockito.verify(view, Mockito.never()).setAccountNames((Set<String>)Matchers.anyObject());
        Mockito.verify(view, Mockito.never()).showGithubPanel();
        Mockito.verify(view, Mockito.never()).setRepositories(Matchers.<List<ProjectData>>anyObject());
    }

    @Test
    public void onLoadRepoClickedWhenAuthorizeIsSuccessful() throws Exception {
        Mockito.doAnswer(new Answer() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                presenter.onFailRequest(promiseError);
                return null;
            }
        }).doAnswer(new Answer() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                presenter.onSuccessRequest(jsArrayMixed);
                return null;
            }
        }).when(presenter).doRequest(Matchers.any(Promise.class), Matchers.any(Promise.class), Matchers.any(Promise.class));
        final Throwable exception = Mockito.mock(UnauthorizedException.class);
        String userId = "userId";
        CurrentUser user = Mockito.mock(CurrentUser.class);
        ProfileDescriptor profile = Mockito.mock(ProfileDescriptor.class);
        Mockito.doReturn(exception).when(promiseError).getCause();

        Mockito.when(appContext.getCurrentUser()).thenReturn(user);
        Mockito.when(user.getProfile()).thenReturn(profile);
        Mockito.when(profile.getId()).thenReturn(userId);

        presenter.onLoadRepoClicked();

        Mockito.verify(gitHubClientService).getRepositoriesList();
        Mockito.verify(gitHubClientService).getUserInfo();
        Mockito.verify(gitHubClientService).getOrganizations();

        Mockito.verify(gitHubAuthenticator).authenticate(Matchers.anyString(), asyncCallbackCaptor.capture());
        AsyncCallback<OAuthStatus> asyncCallback = asyncCallbackCaptor.getValue();
        asyncCallback.onSuccess(null);

        Mockito.verify(view, Mockito.times(3)).setLoaderVisibility(Matchers.eq(true));
        Mockito.verify(view, Mockito.times(3)).setInputsEnableState(Matchers.eq(false));
        Mockito.verify(view, Mockito.times(3)).setInputsEnableState(Matchers.eq(true));
    }

    private void verifyInvocationsForCorrectUrl(String correctUrl) {
        Mockito.verify(view, Mockito.never()).markURLInvalid();
        Mockito.verify(source).setLocation(Matchers.eq(correctUrl));
        Mockito.verify(view).markURLValid();
        Mockito.verify(view).setProjectName(Matchers.anyString());
        Mockito.verify(updateDelegate).updateControls();
    }

}
