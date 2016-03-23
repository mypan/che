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
package org.eclipse.che.ide.ext.github.client.authenticator;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.ssh.gwt.client.SshServiceClient;
import org.eclipse.che.api.ssh.shared.dto.SshPairDto;
import org.eclipse.che.api.user.shared.dto.ProfileDescriptor;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.app.CurrentUser;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.ext.github.client.GitHubLocalizationConstant;
import org.eclipse.che.ide.ext.git.ssh.client.GitSshKeyUploaderRegistry;
import org.eclipse.che.ide.ext.git.ssh.client.SshKeyUploader;
import org.eclipse.che.ide.ext.git.ssh.client.manage.SshKeyManagerPresenter;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.ui.dialogs.ConfirmCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.ui.dialogs.message.MessageDialog;
import org.eclipse.che.security.oauth.OAuthStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.che.ide.api.notification.StatusNotification.Status.SUCCESS;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Testing {@link GitHubAuthenticatorImpl} functionality.
 *
 * @author Roman Nikitenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class GitHubAuthenticatorImplTest {
    public static final String GITHUB_HOST = "github.com";

    @Captor
    private ArgumentCaptor<AsyncCallback<Void>> generateKeyCallbackCaptor;

    @Captor
    private ArgumentCaptor<Operation<List<SshPairDto>>> operationSshPairDTOsCapture;

    @Captor
    private ArgumentCaptor<Operation<Void>> operationVoid;

    private Promise<List<SshPairDto>> sshPairDTOsPromise;

    private Promise<Void> voidPromise;

    @Mock
    private GitHubAuthenticatorView    view;
    @Mock
    private SshServiceClient           sshServiceClient;
    @Mock
    private DialogFactory              dialogFactory;
    @Mock
    private DtoUnmarshallerFactory     dtoUnmarshallerFactory;
    @Mock
    private NotificationManager        notificationManager;
    @Mock
    private GitHubLocalizationConstant locale;
    @Mock
    private AppContext                 appContext;
    @Mock
    private GitSshKeyUploaderRegistry  registry;
    @InjectMocks
    private GitHubAuthenticatorImpl    gitHubAuthenticator;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        sshPairDTOsPromise = createPromise();
        voidPromise = createPromise();

        Mockito.when(sshServiceClient.getPairs(Matchers.anyString())).thenReturn(sshPairDTOsPromise);
        Mockito.when(sshServiceClient.deletePair(Matchers.anyString(), Matchers.anyString())).thenReturn(voidPromise);
    }

    private Promise createPromise() {
        return Mockito.mock(Promise.class, new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                if (invocation.getMethod().getReturnType().isInstance(invocation.getMock())) {
                    return invocation.getMock();
                }
                return Mockito.RETURNS_DEFAULTS.answer(invocation);
            }
        });
    }

    @Test
    public void delegateShouldBeSet() throws Exception {
        Mockito.verify(view).setDelegate(gitHubAuthenticator);
    }

    @Test
    public void dialogShouldBeShow() throws Exception {
        AsyncCallback<OAuthStatus> callback = getCallBack();
        gitHubAuthenticator.authenticate(null, callback);

        Mockito.verify(view).showDialog();
        Assert.assertThat(gitHubAuthenticator.callback, Is.is(callback));
    }

    @Test
    public void onAuthenticatedWhenGenerateKeysIsSelected() throws Exception {
        String userId = "userId";
        OAuthStatus authStatus = Mockito.mock(OAuthStatus.class);

        SshKeyUploader sshKeyUploader = Mockito.mock(SshKeyUploader.class);

        CurrentUser user = Mockito.mock(CurrentUser.class);
        ProfileDescriptor profile = Mockito.mock(ProfileDescriptor.class);
        Mockito.when(view.isGenerateKeysSelected()).thenReturn(true);

        Mockito.when(registry.getUploader(GITHUB_HOST)).thenReturn(sshKeyUploader);

        Mockito.when(appContext.getCurrentUser()).thenReturn(user);
        Mockito.when(user.getProfile()).thenReturn(profile);
        Mockito.when(profile.getId()).thenReturn(userId);

        gitHubAuthenticator.onAuthenticated(authStatus);

        Mockito.verify(view).isGenerateKeysSelected();
        Mockito.verify(registry).getUploader(Matchers.eq(GITHUB_HOST));
        Mockito.verify(appContext).getCurrentUser();
        Mockito.verify(sshKeyUploader).uploadKey(Matchers.eq(userId), Matchers.<AsyncCallback<Void>>anyObject());
    }

    @Test
    public void onAuthenticatedWhenGenerateKeysIsNotSelected() throws Exception {
        String userId = "userId";
        OAuthStatus authStatus = Mockito.mock(OAuthStatus.class);

        CurrentUser user = Mockito.mock(CurrentUser.class);
        ProfileDescriptor profile = Mockito.mock(ProfileDescriptor.class);
        Mockito.when(view.isGenerateKeysSelected()).thenReturn(false);
        Mockito.when(appContext.getCurrentUser()).thenReturn(user);
        Mockito.when(user.getProfile()).thenReturn(profile);
        Mockito.when(profile.getId()).thenReturn(userId);

        gitHubAuthenticator.authenticate(null, getCallBack());
        gitHubAuthenticator.onAuthenticated(authStatus);

        Mockito.verify(view).isGenerateKeysSelected();
        Mockito.verifyNoMoreInteractions(registry);
    }

    @Test
    public void onAuthenticatedWhenGenerateKeysIsSuccess() throws Exception {
        String userId = "userId";
        OAuthStatus authStatus = Mockito.mock(OAuthStatus.class);
        SshKeyUploader keyProvider = Mockito.mock(SshKeyUploader.class);

        CurrentUser user = Mockito.mock(CurrentUser.class);
        ProfileDescriptor profile = Mockito.mock(ProfileDescriptor.class);
        Mockito.when(view.isGenerateKeysSelected()).thenReturn(true);
        Mockito.when(registry.getUploader(GITHUB_HOST)).thenReturn(keyProvider);

        CurrentProject currentProject = Mockito.mock(CurrentProject.class);
        ProjectConfigDto projectConfigDto = Mockito.mock(ProjectConfigDto.class);
        Mockito.when(appContext.getCurrentProject()).thenReturn(currentProject);
        Mockito.when(currentProject.getRootProject()).thenReturn(projectConfigDto);

        Mockito.when(appContext.getCurrentUser()).thenReturn(user);
        Mockito.when(user.getProfile()).thenReturn(profile);
        Mockito.when(profile.getId()).thenReturn(userId);

        gitHubAuthenticator.authenticate(null, getCallBack());
        gitHubAuthenticator.onAuthenticated(authStatus);

        Mockito.verify(keyProvider).uploadKey(Matchers.eq(userId), generateKeyCallbackCaptor.capture());
        AsyncCallback<Void> generateKeyCallback = generateKeyCallbackCaptor.getValue();
        generateKeyCallback.onSuccess(null);

        Mockito.verify(view).isGenerateKeysSelected();
        Mockito.verify(registry).getUploader(Matchers.eq(GITHUB_HOST));
        Mockito.verify(appContext).getCurrentUser();
        Mockito.verify(notificationManager).notify(Matchers.anyString(), Matchers.eq(Status.SUCCESS), Matchers.eq(true));
    }

    @Test
    public void onAuthenticatedWhenGenerateKeysIsFailure() throws Exception {
        String userId = "userId";
        OAuthStatus authStatus = Mockito.mock(OAuthStatus.class);

        SshKeyUploader keyProvider = Mockito.mock(SshKeyUploader.class);

        CurrentUser user = Mockito.mock(CurrentUser.class);
        ProfileDescriptor profile = Mockito.mock(ProfileDescriptor.class);
        MessageDialog messageDialog = Mockito.mock(MessageDialog.class);
        Mockito.when(view.isGenerateKeysSelected()).thenReturn(true);
        Mockito.when(registry.getUploader(GITHUB_HOST)).thenReturn(keyProvider);

        Mockito.when(appContext.getCurrentUser()).thenReturn(user);
        Mockito.when(user.getProfile()).thenReturn(profile);
        Mockito.when(profile.getId()).thenReturn(userId);
        Mockito.when(dialogFactory.createMessageDialog(Matchers.anyString(), Matchers.anyString(), Matchers.<ConfirmCallback>anyObject())).thenReturn(messageDialog);

        gitHubAuthenticator.authenticate(null, getCallBack());
        gitHubAuthenticator.onAuthenticated(authStatus);

        Mockito.verify(keyProvider).uploadKey(Matchers.eq(userId), generateKeyCallbackCaptor.capture());
        AsyncCallback<Void> generateKeyCallback = generateKeyCallbackCaptor.getValue();
        generateKeyCallback.onFailure(new Exception(""));

        Mockito.verify(view).isGenerateKeysSelected();
        Mockito.verify(registry).getUploader(Matchers.eq(GITHUB_HOST));
        Mockito.verify(appContext).getCurrentUser();
        Mockito.verify(dialogFactory).createMessageDialog(Matchers.anyString(), Matchers.anyString(), Matchers.<ConfirmCallback>anyObject());
        Mockito.verify(messageDialog).show();
        Mockito.verify(sshServiceClient).getPairs(Matchers.eq(SshKeyManagerPresenter.GIT_SSH_SERVICE));
    }

    @Test
    public void onAuthenticatedWhenGetFailedKeyIsSuccess() throws Exception {
        String userId = "userId";
        SshPairDto pair = Mockito.mock(SshPairDto.class);
        List<SshPairDto> pairs = new ArrayList<>();
        pairs.add(pair);
        OAuthStatus authStatus = Mockito.mock(OAuthStatus.class);
        SshKeyUploader keyUploader = Mockito.mock(SshKeyUploader.class);

        CurrentUser user = Mockito.mock(CurrentUser.class);
        ProfileDescriptor profile = Mockito.mock(ProfileDescriptor.class);
        MessageDialog messageDialog = Mockito.mock(MessageDialog.class);
        Mockito.when(view.isGenerateKeysSelected()).thenReturn(true);
        Mockito.when(registry.getUploader(GITHUB_HOST)).thenReturn(keyUploader);

        Mockito.when(appContext.getCurrentUser()).thenReturn(user);
        Mockito.when(user.getProfile()).thenReturn(profile);
        Mockito.when(profile.getId()).thenReturn(userId);
        Mockito.when(dialogFactory.createMessageDialog(Matchers.anyString(), Matchers.anyString(), Matchers.<ConfirmCallback>anyObject())).thenReturn(messageDialog);
        Mockito.when(pair.getName()).thenReturn(GITHUB_HOST);
        Mockito.when(pair.getService()).thenReturn(SshKeyManagerPresenter.GIT_SSH_SERVICE);

        gitHubAuthenticator.authenticate(null, getCallBack());
        gitHubAuthenticator.onAuthenticated(authStatus);

        Mockito.verify(keyUploader).uploadKey(Matchers.eq(userId), generateKeyCallbackCaptor.capture());
        AsyncCallback<Void> generateKeyCallback = generateKeyCallbackCaptor.getValue();
        generateKeyCallback.onFailure(new Exception(""));

        Mockito.verify(sshPairDTOsPromise).then(operationSshPairDTOsCapture.capture());
        operationSshPairDTOsCapture.getValue().apply(pairs);

        Mockito.verify(view).isGenerateKeysSelected();
        Mockito.verify(registry).getUploader(Matchers.eq(GITHUB_HOST));
        Mockito.verify(appContext).getCurrentUser();
        Mockito.verify(dialogFactory).createMessageDialog(Matchers.anyString(), Matchers.anyString(), Matchers.<ConfirmCallback>anyObject());
        Mockito.verify(messageDialog).show();
        Mockito.verify(sshServiceClient).getPairs(Matchers.eq(SshKeyManagerPresenter.GIT_SSH_SERVICE));
        Mockito.verify(sshServiceClient).deletePair(Matchers.eq(SshKeyManagerPresenter.GIT_SSH_SERVICE), Matchers.eq(GITHUB_HOST));
    }

    private AsyncCallback<OAuthStatus> getCallBack() {
        return new AsyncCallback<OAuthStatus>() {
            @Override
            public void onFailure(Throwable caught) {

            }

            @Override
            public void onSuccess(OAuthStatus result) {

            }
        };
    }
}
