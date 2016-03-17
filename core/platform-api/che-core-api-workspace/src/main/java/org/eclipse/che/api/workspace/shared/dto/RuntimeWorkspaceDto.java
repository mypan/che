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
package org.eclipse.che.api.workspace.shared.dto;

import org.eclipse.che.api.core.model.workspace.WorkspaceStatus;
import org.eclipse.che.api.core.rest.shared.dto.Hyperlinks;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.machine.shared.dto.MachineDto;
import org.eclipse.che.dto.shared.DTO;

import java.util.List;

// TODO refactor CHE-718


/**
 * @author Alexander Garagatyi
 */
@DTO
public interface RuntimeWorkspaceDto extends /*RuntimeWorkspace*/ Hyperlinks {

//    @Override
//    WorkspaceConfigDto getConfig();
//
//    RuntimeWorkspaceDto withConfig(WorkspaceConfigDto config);
//
//    RuntimeWorkspaceDto withId(String id);
//
//    RuntimeWorkspaceDto withOwner(String owner);
//
//    RuntimeWorkspaceDto withStatus(WorkspaceStatus status);
//
//    RuntimeWorkspaceDto withTemporary(boolean isTemporary);
//
//    RuntimeWorkspaceDto withActiveEnv(String activeEnvName);
//
//    @Override
//    MachineDto getDevMachine();
//
//    RuntimeWorkspaceDto withDevMachine(MachineDto machine);
//
//    @Override
//    List<MachineDto> getMachines();
//
//    RuntimeWorkspaceDto withMachines(List<MachineDto> machines);
//
//    RuntimeWorkspaceDto withRootFolder(String rootFolder);
//
//    @Override
//    RuntimeWorkspaceDto withLinks(List<Link> links);
}
