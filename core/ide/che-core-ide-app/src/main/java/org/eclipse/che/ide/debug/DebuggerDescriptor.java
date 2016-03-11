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

/**
 * Represents general information about debugger.
 *
 * @author Anatoliy Bazko
 */
public class DebuggerDescriptor {

    private final String info;
    private final String address;

    public DebuggerDescriptor(String info, String address) {
        this.info = info;
        this.address = address;
    }

    public String getInfo() {
        return info;
    }

    public String getAddress() {
        return address;
    }
}
