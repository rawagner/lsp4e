/*******************************************************************************
 * Copyright (c) 2016, 2017 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Mickael Istria (Red Hat Inc.) - initial implementation
 *  Miro Spoenemann (TypeFox) - extracted LanguageClientImpl
 *  Jan Koehnlein (TypeFox) - bug 521744
 *******************************************************************************/
package org.eclipse.lsp4e;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.lsp4e.LanguageServersRegistry.LanguageServerDefinition;

/**
 * Wraps instantiation, initialization of project-specific instance of the
 * language server
 * 
 * @deprecated use {@link LanguageServerWrapper}
 */
@Deprecated
public class ProjectSpecificLanguageServerWrapper extends LanguageServerWrapper {

	public ProjectSpecificLanguageServerWrapper(@NonNull IProject project,
			@NonNull LanguageServerDefinition serverDefinition) {
		super(project, serverDefinition);
	}

}
