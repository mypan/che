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

import com.google.inject.Inject;

import org.eclipse.che.api.machine.gwt.client.events.WsAgentStateEvent;
import org.eclipse.che.api.machine.gwt.client.events.WsAgentStateHandler;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.promises.client.js.JsPromise;
import org.eclipse.che.api.promises.client.js.JsPromiseError;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.debug.Breakpoint;
import org.eclipse.che.ide.debug.Debugger;
import org.eclipse.che.ide.debug.DebuggerDescriptor;
import org.eclipse.che.ide.debug.DebuggerObservable;
import org.eclipse.che.ide.debug.DebuggerObserver;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.java.client.projecttree.JavaSourceFolderUtil;
import org.eclipse.che.ide.ext.java.jdi.client.JavaRuntimeExtension;
import org.eclipse.che.ide.ext.java.jdi.client.fqn.FqnResolver;
import org.eclipse.che.ide.ext.java.jdi.client.fqn.FqnResolverFactory;
import org.eclipse.che.ide.ext.java.jdi.client.marshaller.DebuggerEventListUnmarshallerWS;
import org.eclipse.che.ide.ext.java.jdi.shared.BreakPoint;
import org.eclipse.che.ide.ext.java.jdi.shared.BreakPointEvent;
import org.eclipse.che.ide.ext.java.jdi.shared.BreakpointActivatedEvent;
import org.eclipse.che.ide.ext.java.jdi.shared.DebuggerEvent;
import org.eclipse.che.ide.ext.java.jdi.shared.DebuggerEventList;
import org.eclipse.che.ide.ext.java.jdi.shared.JavaDebuggerConnectionContext;
import org.eclipse.che.ide.ext.java.jdi.shared.JavaDebuggerInfo;
import org.eclipse.che.ide.ext.java.jdi.shared.Location;
import org.eclipse.che.ide.ext.java.jdi.shared.StackFrameDump;
import org.eclipse.che.ide.ext.java.jdi.shared.StepEvent;
import org.eclipse.che.ide.ext.java.jdi.shared.UpdateVariableRequest;
import org.eclipse.che.ide.ext.java.jdi.shared.Value;
import org.eclipse.che.ide.ext.java.jdi.shared.Variable;
import org.eclipse.che.ide.ext.java.jdi.shared.VariablePath;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.HTTPStatus;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.util.storage.LocalStorage;
import org.eclipse.che.ide.util.storage.LocalStorageProvider;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.MessageBusProvider;
import org.eclipse.che.ide.websocket.WebSocketException;
import org.eclipse.che.ide.websocket.rest.SubscriptionHandler;
import org.eclipse.che.ide.websocket.rest.exceptions.ServerException;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.eclipse.che.ide.ext.java.jdi.shared.DebuggerEvent.BREAKPOINT;
import static org.eclipse.che.ide.ext.java.jdi.shared.DebuggerEvent.BREAKPOINT_ACTIVATED;
import static org.eclipse.che.ide.ext.java.jdi.shared.DebuggerEvent.STEP;

/**
 * The java debugger.
 *
 * @author Anatoliy Bazko
 */
public class JavaDebugger implements Debugger<JavaDebuggerConnectionContext>, DebuggerObservable {

    private static final String            LOCAL_STORAGE_DEBUGGER_KEY = "che-java-debugger";
    public static final  JsPromise<String> DEBUGGER_NOT_CONNECTED     = Promises.reject(JsPromiseError.create("Debugger is not connected"));

    private final List<DebuggerObserver>        observers;
    private final JavaDebuggerServiceClientImpl service;
    private final DtoFactory                    dtoFactory;
    private final LocalStorageProvider          localStorageProvider;
    private final EventBus                      eventBus;
    private final FqnResolverFactory            fqnResolverFactory;
    private final AppContext                    appContext;
    private final JavaDebuggerFileHandler       javaDebuggerFileHandler;

    /** Channel identifier to receive events from debugger over WebSocket. */
    private String debuggerEventsChannel;
    /** Channel identifier to receive event when debugger will be disconnected. */
    private String debuggerDisconnectedChannel;

    private JavaDebuggerInfo                       javaDebuggerInfo;
    private SubscriptionHandler<DebuggerEventList> debuggerEventsHandler;
    private SubscriptionHandler<Void>              debuggerDisconnectedHandler;

    private MessageBus messageBus;

    @Inject
    public JavaDebugger(JavaDebuggerServiceClientImpl service,
                        DtoFactory dtoFactory,
                        LocalStorageProvider localStorageProvider,
                        MessageBusProvider messageBusProvider,
                        EventBus eventBus,s
                        FqnResolverFactory fqnResolverFactory,
                        AppContext appContext,
                        JavaDebuggerFileHandler javaDebuggerFileHandler) {
        this.service = service;
        this.dtoFactory = dtoFactory;
        this.localStorageProvider = localStorageProvider;
        this.eventBus = eventBus;
        this.fqnResolverFactory = fqnResolverFactory;
        this.appContext = appContext;
        this.javaDebuggerFileHandler = javaDebuggerFileHandler;
        this.observers = new ArrayList<>();

        addHandlers(messageBusProvider);
    }

    private void addHandlers(final MessageBusProvider messageBusProvider) {
        eventBus.addHandler(WsAgentStateEvent.TYPE, new WsAgentStateHandler() {
            @Override
            public void onWsAgentStarted(WsAgentStateEvent event) {
                messageBus = messageBusProvider.getMachineMessageBus();

                restoreDebuggerInfo();
                if (isConnected()) {
                    service.checkEvents(javaDebuggerInfo.getId(), new AsyncRequestCallback<DebuggerEventList>() {
                        @Override
                        protected void onSuccess(DebuggerEventList result) {
                            startCheckingEvents();
                        }

                        @Override
                        protected void onFailure(Throwable exception) {
                            invalidateDebuggerInfo();
                            preserveDebuggerInfo();
                        }
                    });
                }
            }

            @Override
            public void onWsAgentStopped(WsAgentStateEvent event) { }
        });

        this.debuggerEventsHandler = new SubscriptionHandler<DebuggerEventList>(new DebuggerEventListUnmarshallerWS(dtoFactory)) {
            @Override
            public void onMessageReceived(DebuggerEventList result) {
                onEventListReceived(result);
            }

            @Override
            public void onErrorReceived(Throwable exception) {
                try {
                    messageBus.unsubscribe(debuggerEventsChannel, this);
                } catch (WebSocketException e) {
                    Log.error(JavaDebugger.class, e);
                }

                if (exception instanceof ServerException) {
                    ServerException serverException = (ServerException)exception;
                    if (HTTPStatus.INTERNAL_ERROR == serverException.getHTTPStatus()
                        && serverException.getMessage() != null
                        && serverException.getMessage().contains("not found")) {

                        disconnectDebugger();
                    }
                }
            }
        };

        this.debuggerDisconnectedHandler = new SubscriptionHandler<Void>() {
            @Override
            protected void onMessageReceived(Void result) {
                try {
                    messageBus.unsubscribe(debuggerDisconnectedChannel, this);
                } catch (WebSocketException e) {
                    Log.error(JavaDebugger.class, e);
                }

                disconnectDebugger();
            }

            @Override
            protected void onErrorReceived(Throwable exception) {
                try {
                    messageBus.unsubscribe(debuggerDisconnectedChannel, this);
                } catch (WebSocketException e) {
                    Log.error(JavaDebugger.class, e);
                }
            }
        };
    }

    private void onEventListReceived(@NotNull DebuggerEventList eventList) {
        Location location;

        List<DebuggerEvent> events = eventList.getEvents();
        for (DebuggerEvent event : events) {
            switch (event.getType()) {
                case STEP:
                    location = ((StepEvent)event).getLocation();
                    break;
                case BREAKPOINT_ACTIVATED:
                    BreakPoint breakPoint = ((BreakpointActivatedEvent)event).getBreakPoint();
                    onBreakpointActivated(breakPoint.getLocation());
                    return;
                case BREAKPOINT:
                    location = ((BreakPointEvent)event).getBreakPoint().getLocation();
                    break;
                default:
                    Log.error(JavaDebugger.class, "Unknown type of debugger event: " + event.getType());
                    return;
            }

            if (location != null) {
                javaDebuggerFileHandler.openFile(resolveFilePathByLocation(location),
                                                 location.getClassName(),
                                                 location.getLineNumber() - 1);
                for (DebuggerObserver observer : observers) {
                    observer.onBreakpointStopped(location.getClassName(), location.getLineNumber() - 1);
                }
            }
        }
    }

    /**
     * Breakpoint became active. It might happens because of different reasons:
     * <li>breakpoint was deferred and VM eventually loaded class and added it</li>
     * <li>condition triggered</li>
     * <li>etc</li>
     */
    private void onBreakpointActivated(Location location) {
        List<String> filePaths = resolveFilePathByLocation(location);
        for (String filePath : filePaths) {
            for (DebuggerObserver observer : observers) {
                observer.onBreakpointActivated(filePath, location.getLineNumber() - 1);
            }
        }
    }

    /**
     * Create file path from {@link Location}.
     *
     * @param location
     *         location of class
     * @return file path
     */
    @NotNull
    private List<String> resolveFilePathByLocation(@NotNull Location location) {
        CurrentProject currentProject = appContext.getCurrentProject();

        if (currentProject == null) {
            return Collections.emptyList();
        }

        String pathSuffix = location.getClassName().replace(".", "/") + ".java";

        List<String> sourceFolders = JavaSourceFolderUtil.getSourceFolders(currentProject);
        List<String> filePaths = new ArrayList<>(sourceFolders.size() + 1);

        for (String sourceFolder : sourceFolders) {
            filePaths.add(sourceFolder + pathSuffix);
        }
        filePaths.add(location.getClassName());

        return filePaths;
    }

    private void startCheckingEvents() {
        debuggerEventsChannel = JavaRuntimeExtension.EVENTS_CHANNEL + javaDebuggerInfo.getId();
        try {
            messageBus.subscribe(debuggerEventsChannel, debuggerEventsHandler);
        } catch (WebSocketException e) {
            Log.error(DebuggerPresenter.class, e);
        }

        try {
            debuggerDisconnectedChannel = JavaRuntimeExtension.DISCONNECT_CHANNEL + javaDebuggerInfo.getId();
            messageBus.subscribe(debuggerDisconnectedChannel, debuggerDisconnectedHandler);
        } catch (WebSocketException e) {
            Log.error(JavaDebugger.class, e);
        }
    }

    private void stopCheckingDebugEvents() {
        try {
            if (messageBus.isHandlerSubscribed(debuggerEventsHandler, debuggerEventsChannel)) {
                messageBus.unsubscribe(debuggerEventsChannel, debuggerEventsHandler);
            }

            if (messageBus.isHandlerSubscribed(debuggerDisconnectedHandler, debuggerDisconnectedChannel)) {
                messageBus.unsubscribe(debuggerDisconnectedChannel, debuggerDisconnectedHandler);
            }
        } catch (WebSocketException e) {
            Log.error(JavaDebugger.class, e);
        }
    }

    @Override
    public Promise<String> getValue(String variable) {
        if (!isConnected()) {
            return DEBUGGER_NOT_CONNECTED;
        }

        Promise<Value> promise = service.getValue(javaDebuggerInfo.getId(), dtoFactory.createDtoFromJson(variable, Variable.class));
        return promise.then(new Function<Value, String>() {
            @Override
            public String apply(Value arg) throws FunctionException {
                List<Variable> variables = arg.getVariables();
                return dtoFactory.toJson(variables);
            }
        });
    }

    @Override
    public Promise<String> getStackFrameDump() {
        if (!isConnected()) {
            return DEBUGGER_NOT_CONNECTED;
        }

        Promise<StackFrameDump> promise = service.getStackFrameDump(javaDebuggerInfo.getId());
        return promise.then(new Function<StackFrameDump, String>() {
            @Override
            public String apply(StackFrameDump arg) throws FunctionException {
                return dtoFactory.toJson(arg);
            }
        });
    }

    @Override
    public void addBreakpoint(final VirtualFile file, final int lineNumber) {
        if (isConnected()) {
            Location location = dtoFactory.createDto(Location.class);
            location.setLineNumber(lineNumber + 1);

            final FqnResolver resolver = fqnResolverFactory.getResolver(file.getMediaType());
            if (resolver != null) {
                location.setClassName(resolver.resolveFqn(file));
            } else {
                return;
            }

            BreakPoint breakPoint = dtoFactory.createDto(BreakPoint.class);
            breakPoint.setLocation(location);
            breakPoint.setEnabled(true);

            Promise<Void> promise = service.addBreakpoint(javaDebuggerInfo.getId(), breakPoint);
            promise.then(new Operation<Void>() {
                @Override
                public void apply(Void arg) throws OperationException {
                    Breakpoint breakpoint = new Breakpoint(Breakpoint.Type.BREAKPOINT, lineNumber, file.getPath(), file, true);
                    for (DebuggerObserver observer : observers) {
                        observer.onBreakpointAdded(breakpoint);
                    }
                }
            }).catchError(new Operation<PromiseError>() {
                @Override
                public void apply(PromiseError arg) throws OperationException {
                    Log.error(JavaDebugger.class, arg.getMessage());
                }
            });
        } else {
            Breakpoint breakpoint = new Breakpoint(Breakpoint.Type.BREAKPOINT, lineNumber, file.getPath(), file, false);
            for (DebuggerObserver observer : observers) {
                observer.onBreakpointAdded(breakpoint);
            }
        }
    }

    @Override
    public void deleteBreakpoint(final VirtualFile file, final int lineNumber) {
        if (isConnected()) {
            Location location = dtoFactory.createDto(Location.class);
            location.setLineNumber(lineNumber + 1);

            FqnResolver resolver = fqnResolverFactory.getResolver(file.getMediaType());
            if (resolver != null) {
                location.setClassName(resolver.resolveFqn(file));
            } else {
                Log.warn(JavaDebugger.class, "FqnResolver is not found");
            }

            BreakPoint point = dtoFactory.createDto(BreakPoint.class);
            point.setLocation(location);
            point.setEnabled(true);

            Promise<Void> promise = service.deleteBreakpoint(javaDebuggerInfo.getId(), point);
            promise.then(new Operation<Void>() {
                @Override
                public void apply(Void arg) throws OperationException {
                    for (DebuggerObserver observer : observers) {
                        Breakpoint breakpoint = new Breakpoint(Breakpoint.Type.BREAKPOINT, lineNumber, file.getPath(), file, false);
                        observer.onBreakpointDeleted(breakpoint);
                    }
                }
            }).catchError(new Operation<PromiseError>() {
                @Override
                public void apply(PromiseError arg) throws OperationException {
                    Log.error(JavaDebugger.class, arg.getMessage());
                }
            });
        }
    }

    @Override
    public void deleteAllBreakpoints() {
        if (isConnected()) {
            Promise<Void> promise = service.deleteAllBreakpoints(javaDebuggerInfo.getId());

            promise.then(new Operation<Void>() {
                @Override
                public void apply(Void arg) throws OperationException {
                    for (DebuggerObserver observer : observers) {
                        observer.onAllBreakpointDeleted();
                    }
                }
            });
        }
    }

    @Override
    public Promise<Void> attachDebugger(@NotNull JavaDebuggerConnectionContext connectionContext) {
        if (isConnected()) {
            return Promises.reject(JsPromiseError.create("Debugger already connected"));
        }

        Promise<JavaDebuggerInfo> connect = service.connect(connectionContext);

        Promise<Void> promise = connect.then(new Function<JavaDebuggerInfo, Void>() {
            @Override
            public Void apply(JavaDebuggerInfo arg) throws FunctionException {
                return null;
            }
        });


        for (DebuggerObserver observer : observers) {
            String info = "";
            String address = connectionContext.getHost() + ":" + connectionContext.getPort();
            DebuggerDescriptor debuggerDescriptor = new DebuggerDescriptor(info, address);
            observer.onDebuggerAttached(debuggerDescriptor, promise);
        }

        connect.then(new Operation<JavaDebuggerInfo>() {
            @Override
            public void apply(JavaDebuggerInfo arg) throws OperationException {
                setJavaDebuggerInfo(arg);
                preserveDebuggerInfo();
                startCheckingEvents();
            }
        });

        return promise;
    }

    @Override
    public void disconnectDebugger() {
        stopCheckingDebugEvents();

        Promise<Void> disconnect;
        if (isConnected()) {
            disconnect = service.disconnect(javaDebuggerInfo.getId());
        } else {
            disconnect = Promises.resolve(null);
        }

        invalidateDebuggerInfo();
        preserveDebuggerInfo();

        disconnect.then(new Operation<Void>() {
            @Override
            public void apply(Void arg) throws OperationException {
                for (DebuggerObserver observer : observers) {
                    observer.onDebuggerDisconnected();
                }
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                Log.error(JavaDebugger.class, arg.getMessage());
                for (DebuggerObserver observer : observers) {
                    observer.onDebuggerDisconnected();
                }
            }
        });
    }

    @Override
    public void stepInto() {
        if (isConnected()) {
            Promise<Void> promise = service.stepInto(javaDebuggerInfo.getId());
            promise.then(new Operation<Void>() {
                @Override
                public void apply(Void arg) throws OperationException {
                    for (DebuggerObserver observer : observers) {
                        observer.onStepIn();
                    }
                }
            }).catchError(new Operation<PromiseError>() {
                @Override
                public void apply(PromiseError arg) throws OperationException {
                    Log.error(JavaDebugger.class, arg.getCause());
                }
            });
        }
    }

    @Override
    public void stepOver() {
        if (isConnected()) {
            Promise<Void> promise = service.stepOver(javaDebuggerInfo.getId());
            promise.then(new Operation<Void>() {
                @Override
                public void apply(Void arg) throws OperationException {
                    for (DebuggerObserver observer : observers) {
                        observer.onStepOver();
                    }
                }
            }).catchError(new Operation<PromiseError>() {
                @Override
                public void apply(PromiseError arg) throws OperationException {
                    Log.error(JavaDebugger.class, arg.getCause());
                }
            });
        }
    }

    @Override
    public void stepOut() {
        if (isConnected()) {
            Promise<Void> promise = service.stepOut(javaDebuggerInfo.getId());
            promise.then(new Operation<Void>() {
                @Override
                public void apply(Void arg) throws OperationException {
                    for (DebuggerObserver observer : observers) {
                        observer.onStepOut();
                    }
                }
            }).catchError(new Operation<PromiseError>() {
                @Override
                public void apply(PromiseError arg) throws OperationException {
                    Log.error(JavaDebugger.class, arg.getCause());
                }
            });
        }
    }

    @Override
    public void resume() {
        if (isConnected()) {
            Promise<Void> promise = service.resume(javaDebuggerInfo.getId());
            promise.then(new Operation<Void>() {
                @Override
                public void apply(Void arg) throws OperationException {
                    for (DebuggerObserver observer : observers) {
                        observer.onResume();
                    }
                }
            }).catchError(new Operation<PromiseError>() {
                @Override
                public void apply(PromiseError arg) throws OperationException {
                    Log.error(JavaDebugger.class, arg.getCause());
                }
            });
        }
    }

    @Override
    public Promise<String> evaluateExpression(String expression) {
        if (isConnected()) {
            return service.evaluateExpression(javaDebuggerInfo.getId(), expression);
        }

        return DEBUGGER_NOT_CONNECTED;
    }

    @Override
    public void changeVariableValue(final List<String> path, final String newValue) {
        if (isConnected()) {
            VariablePath variablePath = dtoFactory.createDto(VariablePath.class);
            variablePath.setPath(path);

            UpdateVariableRequest updateVariableRequest = dtoFactory.createDto(UpdateVariableRequest.class);
            updateVariableRequest.setVariablePath(variablePath);
            updateVariableRequest.setExpression(newValue);

            Promise<Void> promise = service.setValue(javaDebuggerInfo.getId(), updateVariableRequest);

            promise.then(new Operation<Void>() {
                @Override
                public void apply(Void arg) throws OperationException {
                    for (DebuggerObserver observer : observers) {
                        observer.onValueChanged(path, newValue);
                    }
                }
            }).catchError(new Operation<PromiseError>() {
                @Override
                public void apply(PromiseError arg) throws OperationException {
                    Log.error(JavaDebugger.class, arg.getMessage());
                }
            });
        }
    }

    @Override
    public boolean isConnected() {
        return javaDebuggerInfo != null;
    }

    @Override
    public void addObserver(DebuggerObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(DebuggerObserver observer) {
        observers.remove(observer);
    }

    private void setJavaDebuggerInfo(JavaDebuggerInfo javaDebuggerInfo) {
        this.javaDebuggerInfo = javaDebuggerInfo;
    }

    private void invalidateDebuggerInfo() {
        this.javaDebuggerInfo = null;
    }

    /**
     * Preserves debugger information into the local storage.
     */
    protected void preserveDebuggerInfo() {
        LocalStorage localStorage = localStorageProvider.get();

        if (localStorage == null) {
            return;
        }

        String data;
        if (!isConnected()) {
            data = "";
        } else {
            data = dtoFactory.toJson(javaDebuggerInfo);
        }

        localStorage.setItem(LOCAL_STORAGE_DEBUGGER_KEY, data);
    }

    /**
     * Loads debugger information from the local storage.
     */
    protected void restoreDebuggerInfo() {
        LocalStorage localStorage = localStorageProvider.get();
        if (localStorage == null) {
            invalidateDebuggerInfo();
            return;
        }

        String data = localStorage.getItem(LOCAL_STORAGE_DEBUGGER_KEY);
        if (data == null || data.isEmpty()) {
            invalidateDebuggerInfo();
            return;
        }

        JavaDebuggerInfo debuggerInfo = dtoFactory.createDtoFromJson(data, JavaDebuggerInfo.class);
        setJavaDebuggerInfo(debuggerInfo);
    }
}
