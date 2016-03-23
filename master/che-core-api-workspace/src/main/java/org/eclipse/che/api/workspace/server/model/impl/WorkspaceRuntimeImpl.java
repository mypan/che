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
package org.eclipse.che.api.workspace.server.model.impl;

import org.eclipse.che.api.core.model.machine.Machine;
import org.eclipse.che.api.core.model.workspace.WorkspaceRuntime;
import org.eclipse.che.api.machine.server.model.impl.MachineImpl;
import org.eclipse.che.commons.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * Data object for {@link WorkspaceRuntime}.
 *
 * <p>If constructor/method argument value is prohibited
 * by {@link WorkspaceRuntime} contract then either
 * {@link NullPointerException}(when required argument is null) or
 * {@link IllegalArgumentException}(when argument is not valid) is thrown.
 *
 * @author Yevhenii Voevodin
 */
public class WorkspaceRuntimeImpl implements WorkspaceRuntime {

    private final String activeEnv;

    private String            rootFolder;
    private MachineImpl       devMachine;
    private List<MachineImpl> machines;

    public WorkspaceRuntimeImpl(String activeEnv) {
        this.activeEnv = activeEnv;
    }

    public WorkspaceRuntimeImpl(WorkspaceRuntime runtime) {
        this(runtime.getActiveEnv());
        this.rootFolder = runtime.getRootFolder();
        if (runtime.getDevMachine() != null) {
            this.devMachine = new MachineImpl(runtime.getDevMachine());
        }
        this.machines = runtime.getMachines()
                               .stream()
                               .map(MachineImpl::new)
                               .collect(toList());
    }

    @Override
    public String getActiveEnv() {
        return activeEnv;
    }

    @Override
    @Nullable
    public String getRootFolder() {
        return rootFolder;
    }

    public void setRootFolder(String rootFolder) {
        this.rootFolder = rootFolder;
    }

    @Override
    @Nullable
    public Machine getDevMachine() {
        return devMachine;
    }

    public void setDevMachine(@Nullable Machine devMachine) {
        if (devMachine != null) {
            this.devMachine = new MachineImpl(devMachine);
        }
    }

    @Override
    public List<MachineImpl> getMachines() {
        if (machines == null) {
            machines = new ArrayList<>();
        }
        return machines;
    }

    public void setMachines(List<MachineImpl> machines) {
        this.machines = machines;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof WorkspaceRuntimeImpl)) {
            return false;
        }
        final WorkspaceRuntimeImpl other = (WorkspaceRuntimeImpl)obj;
        return activeEnv.equals(other.activeEnv)
               && Objects.equals(rootFolder, other.rootFolder)
               && Objects.equals(devMachine, other.devMachine)
               && getMachines().equals(getMachines());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = hash * 31 + activeEnv.hashCode();
        hash = 31 * hash + Objects.hashCode(rootFolder);
        hash = 31 * hash + Objects.hashCode(devMachine);
        hash = 31 * hash + getMachines().hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return "WorkspaceRuntimeImpl{" +
               "activeEnv='" + activeEnv + '\'' +
               ", rootFolder='" + rootFolder + '\'' +
               ", devMachine=" + devMachine +
               ", machines=" + machines +
               '}';
    }
}
