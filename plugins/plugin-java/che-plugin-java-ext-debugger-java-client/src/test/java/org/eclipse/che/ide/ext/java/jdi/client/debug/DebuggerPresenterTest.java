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
package org.eclipse.che.ide.ext.java.jdi.client.debug;

import com.google.common.collect.ImmutableList;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.parts.WorkspaceAgent;
import org.eclipse.che.ide.debug.Breakpoint;
import org.eclipse.che.ide.debug.BreakpointManager;
import org.eclipse.che.ide.debug.DebuggerManager;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.java.jdi.client.BaseTest;
import org.eclipse.che.ide.ext.java.jdi.client.JavaRuntimeLocalizationConstant;
import org.eclipse.che.ide.ext.java.jdi.client.JavaRuntimeResources;
import org.eclipse.che.ide.ext.java.jdi.client.fqn.FqnResolverFactory;
import org.eclipse.che.ide.ext.java.jdi.shared.Variable;
import org.eclipse.che.ide.ui.toolbar.ToolbarPresenter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.che.ide.api.notification.StatusNotification.Status.FAIL;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Testing {@link DebuggerPresenter} functionality.
 *
 * @author Dmytro Nochevnov
 */
public class DebuggerPresenterTest extends BaseTest {
    public static final String ERROR_MESSAGE = "error message";

    @Mock
    private DebuggerView                    view;
    @Mock
    private JavaRuntimeLocalizationConstant constant;
    @Mock
    private BreakpointManager breakpointManager;
    @Mock
    private NotificationManager notificationManager;
    @Mock
    private JavaRuntimeResources javaRuntimeResources;
    @Mock
    @DebuggerToolbar
    private ToolbarPresenter debuggerToolbar;
    @Mock
    private FqnResolverFactory fqnResolverFactory;
    @Mock
    private DtoFactory dtoFactory;
    @Mock
    private DebuggerManager debuggerManager;
    @Mock
    private WorkspaceAgent workspaceAgent;

    @Mock
    private JavaDebugger     debugger;
    @Mock
    private DebuggerVariable selectedVariable;

    @Mock
    private Promise<String> promiseString;
    @Mock
    private Promise<Void>  promiseVoid;
    @Mock
    private PromiseError    promiseError;


    @Captor
    private ArgumentCaptor<Operation<PromiseError>> operationPromiseErrorCaptor;
    @Captor
    private ArgumentCaptor<Operation<Void>>         operationVoidCaptor;
    @Captor
    private ArgumentCaptor<Operation<String>>       operationStringCaptor;

    private DebuggerPresenter presenter;

    @Before
    public void setup() {
        doReturn(debugger).when(debuggerManager).getActiveDebugger();

        doReturn(ERROR_MESSAGE).when(promiseError).getMessage();

        presenter = new DebuggerPresenter(view, constant, breakpointManager, notificationManager, javaRuntimeResources, debuggerToolbar, fqnResolverFactory, dtoFactory, debuggerManager, workspaceAgent);

        presenter.onSelectedVariableElement(selectedVariable);
    }

    @Test
    public void testGo() {
        AcceptsOneWidget container = mock(AcceptsOneWidget.class);
        List<Breakpoint> breakpoinst = new ArrayList<>();

        doReturn(breakpoinst).when(breakpointManager).getBreakpointList();
        doReturn(container).when(view).getDebuggerToolbarPanel();

        presenter.go(container);

        verify(view).setBreakpoints(breakpoinst);
        verify(view).setVariables((List<DebuggerVariable>)any());
        verify(container).setWidget(view);
        verify(debuggerToolbar).go(container);
    }

    @Test
    public void testOnExpandVariablesTree() throws OperationException {
        final String json = "json";

        List<DebuggerVariable> rootVariables = mock(List.class);
        doReturn(true).when(rootVariables).isEmpty();
        doReturn(rootVariables).when(selectedVariable).getVariables();

        Variable variable = mock(Variable.class);
        doReturn(variable).when(selectedVariable).getVariable();
        doReturn(json).when(dtoFactory).toJson(variable);

        List<Variable> variables = ImmutableList.of(variable);
        doReturn(variables).when(dtoFactory).createListDtoFromJson(json, Variable.class);

        doReturn(promiseString).when(debugger).getValue(json);
        doReturn(promiseString).when(promiseString).then((Operation<String>)any());

        presenter.onExpandVariablesTree();

        verify(promiseString).then(operationStringCaptor.capture());
        operationStringCaptor.getValue().apply(json);
        verify(view).setVariablesIntoSelectedVariable((List<DebuggerVariable>) any());
        verify(view).updateSelectedVariable();

        verify(promiseString).catchError(operationPromiseErrorCaptor.capture());
        operationPromiseErrorCaptor.getValue().apply(promiseError);
        notificationManager.notify(any(), eq(ERROR_MESSAGE), eq(FAIL), eq(true));
        verify(constant).failedToGetVariableValueTitle();
    }

//    @Test
    public void testOnSelectedVariableElement() {
        // TODO
    }

//    @Test
    public void testShowAndUpdateView() {
        // TODO
    }

//    @Test
    public void testUpdateStackFrameDump() {
        // TODO
    }

//    @Test
    public void testOnDebuggerAttached() {
        // TODO
    }

//    @Test
    public void testOnDebuggerDisconnected() {
        // TODO
    }

//    @Test
    public void testOnPreSteps() {
        // TODO
    }

//    @Test
    public void testOnBreakpointOperations() {
        // TODO
    }

//    @Test
    public void testOnValueChanged() {
        // TODO
    }

//    @Test
    public void testOnFqnResolverAdded() {
        // TODO
    }
}
