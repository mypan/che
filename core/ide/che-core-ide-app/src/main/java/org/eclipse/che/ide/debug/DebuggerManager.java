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
package org.eclipse.che.ide.debug;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.commons.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The debugger provider.
 *
 * @author Andrey Plotnikov
 * @author Anatoliy Bazko
 */
@Singleton
public class DebuggerManager implements DebuggerManagerObservable, DebuggerObserver {
    private Debugger                      activeDebugger;
    private Map<String, Debugger>         debuggers;
    private List<DebuggerManagerObserver> observers;

    @Inject
    protected DebuggerManager() {
        this.debuggers = new HashMap<>();
        this.observers = new ArrayList<>();
    }

    /**
     * Register new debugger for the given project type ID.
     */
    public void registeredDebugger(String projectTypeId, Debugger debugger) {
        debuggers.put(projectTypeId, debugger);
    }

    /**
     * Gets {@link Debugger} for the given project type ID.
     */
    @Nullable
    public Debugger getDebugger(String projectTypeId) {
        return debuggers.get(projectTypeId);
    }

    /**
     * Sets new active debugger.
     * Resubscriber all {@link DebuggerObserver} to listen to events from new {@link Debugger}
     *
     * @param debugger
     *         debugger is being used
     */
    public void setActiveDebugger(Debugger debugger) {
        if (activeDebugger != null) {
            for (DebuggerManagerObserver observer : observers) {
                activeDebugger.removeObserver(observer);
                activeDebugger.removeObserver(this);
            }
        }

        activeDebugger = debugger;

        if (activeDebugger != null) {
            for (DebuggerManagerObserver observer : observers) {
                activeDebugger.addObserver(observer);
                activeDebugger.addObserver(this);
                observer.onActiveDebuggerChanged(activeDebugger);
            }
        }
    }

    /**
     * @return {@link Debugger} is currently being used
     */
    @Nullable
    public Debugger getActiveDebugger() {
        return activeDebugger;
    }

    // Active debugger events

    @Override
    public void onDebuggerDisconnected() { }

    @Override
    public void addObserver(DebuggerManagerObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(DebuggerManagerObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void onDebuggerAttached(DebuggerDescriptor debuggerDescriptor, Promise<Void> connect) { }

    @Override
    public void onBreakpointAdded(Breakpoint breakpoint) { }

    @Override
    public void onBreakpointActivated(String filePath, int lineNumber) { }

    @Override
    public void onBreakpointDeleted(Breakpoint breakpoint) { }

    @Override
    public void onAllBreakpointDeleted() { }

    @Override
    public void onPreStepIn() { }

    @Override
    public void onPreStepOut() { }

    @Override
    public void onPreStepOver() { }

    @Override
    public void onPreResume() { }

    @Override
    public void onBreakpointStopped(String filePath, String className, int lineNumber) { }

    @Override
    public void onValueChanged(List<String> path, String newValue) { }
}