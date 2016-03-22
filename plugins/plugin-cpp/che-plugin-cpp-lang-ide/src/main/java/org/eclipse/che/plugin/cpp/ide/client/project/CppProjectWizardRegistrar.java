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
package org.eclipse.che.plugin.cpp.ide.client.project;

import com.google.inject.Provider;

import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.ide.api.project.type.wizard.ProjectWizardRegistrar;
import org.eclipse.che.ide.api.wizard.WizardPage;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

import static org.eclipse.che.plugin.cpp.shared.Constants.CPP;
import static org.eclipse.che.plugin.cpp.shared.Constants.C_CATEGORY;


/**
 * Provides information for registering C++ project type into project wizard.
 *
 * @author Vitalii Parfonov
 */
public class CppProjectWizardRegistrar implements ProjectWizardRegistrar {

    private final List<Provider<? extends WizardPage<ProjectConfigDto>>> wizardPages;

    public CppProjectWizardRegistrar() {
        wizardPages = new ArrayList<>();
    }

    @NotNull
    public String getProjectTypeId() {
        return CPP;
    }

    @NotNull
    public String getCategory() {
        return C_CATEGORY;
    }

    @NotNull
    public List<Provider<? extends WizardPage<ProjectConfigDto>>> getWizardPages() {
        return wizardPages;
    }
}
