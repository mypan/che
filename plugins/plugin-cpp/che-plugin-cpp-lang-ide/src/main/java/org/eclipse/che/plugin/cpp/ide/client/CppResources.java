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
package org.eclipse.che.plugin.cpp.ide.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;

import org.vectomatic.dom.svg.ui.SVGResource;

/**
 * @author Vitalii Parfonov
 */
public interface CppResources extends ClientBundle {
    CppResources INSTANCE = GWT.create(CppResources.class);

    @Source("svg/cfile.svg")
    SVGResource cFile();

    @Source("svg/cppfile.svg")
    SVGResource cppFile();

    @Source("svg/category.svg")
    SVGResource category();
}
