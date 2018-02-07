/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Rastislav Wagner (Red Hat Inc.) - initial implementation
 *******************************************************************************/
package org.eclipse.lsp4e.ui;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.lsp4e.ContentTypeToLanguageServerDefinition;
import org.eclipse.lsp4e.LanguageServersRegistry.LanguageServerDefinition;
import org.eclipse.lsp4e.LanguageServiceAccessor;

public class EnableDisableLSJob extends Job {

	private List<ContentTypeToLanguageServerDefinition> serverDefinitions;

	public EnableDisableLSJob(List<ContentTypeToLanguageServerDefinition> serverDefinitions) {
		super(Messages.enableDisableLSJob);
		this.serverDefinitions = serverDefinitions;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		for (ContentTypeToLanguageServerDefinition changedDefinition : serverDefinitions) {
			LanguageServerDefinition serverDefinition = changedDefinition.getValue();
			if (serverDefinition != null) {
				if (!changedDefinition.isEnabled()) {
					LanguageServiceAccessor.disableLanguageServerContentType(changedDefinition);
				} else {
					LanguageServiceAccessor.enableLanguageServerContentType(changedDefinition);
				}
			}
		}
		return Status.OK_STATUS;
	}


}
