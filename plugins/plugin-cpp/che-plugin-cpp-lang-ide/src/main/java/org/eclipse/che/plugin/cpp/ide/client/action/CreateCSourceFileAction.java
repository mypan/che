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
package org.eclipse.che.plugin.cpp.ide.client.action;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.newresource.AbstractNewResourceAction;
import org.eclipse.che.plugin.cpp.ide.client.CppLocalizationConstant;
import org.eclipse.che.plugin.cpp.ide.client.CppResources;

import static org.eclipse.che.plugin.cpp.shared.Constants.C;


/**
 * Action to create new C++ source file.
 *
 * @author Vitalii Parfonov
 */
@Singleton
public class CreateCSourceFileAction extends AbstractNewResourceAction {

    private static final String DEFAULT_CONTENT = "#include <stdio.h>\n" +
                                                  "\n" +
                                                  "int main(void)\n" +
                                                  "{\n" +
                                                  "    printf(\"hello, world\\n\");\n" +
                                                  "}";

    @Inject
    public CreateCSourceFileAction(CppLocalizationConstant localizationConstant,
                                   CppResources cppResources) {
        super(localizationConstant.createCFileActionTitle(),
              localizationConstant.createCFileActionDescription(),
              cppResources.cFile());
    }

    @Override
    protected String getExtension() {
        return C;
    }

    @Override
    protected String getDefaultContent() {
        return DEFAULT_CONTENT;
    }
}
