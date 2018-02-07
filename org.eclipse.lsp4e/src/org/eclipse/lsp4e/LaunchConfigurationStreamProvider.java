/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Mickael Istria (Red Hat Inc.) - initial implementation
 *******************************************************************************/
package org.eclipse.lsp4e;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.lsp4e.server.StreamConnectionProvider;

/**
 * Access and control IO streams from a Launch Configuration to connect
 * them to language server protocol client.
 */
public class LaunchConfigurationStreamProvider implements StreamConnectionProvider  {

	private StreamProxyInputStream inputStream;
	private StreamProxyInputStream errorStream;
	private OutputStream outputStream;
	private ILaunch launch;
	private IProcess process;
	private ILaunchConfiguration launchConfiguration;
	private Set<String> launchModes;

	protected static class StreamProxyInputStream extends InputStream implements IStreamListener {

		private ConcurrentLinkedQueue<Byte> queue = new ConcurrentLinkedQueue<>();
		private IProcess process;

		public StreamProxyInputStream(IProcess process) {
			this.process = process;
		}

		@Override
		public void streamAppended(String text, IStreamMonitor monitor) {
			byte[] bytes = text.getBytes(Charset.defaultCharset());
			List<Byte> bytesAsList = new ArrayList<>(bytes.length);
			for (byte b : bytes) {
				bytesAsList.add(b);
			}
			queue.addAll(bytesAsList);
		}

		@Override
		public int read() throws IOException {
			while (queue.isEmpty()) {
				if (this.process.isTerminated()) {
					return -1;
				}
				try {
					Thread.sleep(5, 0);
				} catch (InterruptedException e) {
					LanguageServerPlugin.logError(e);
				}
			}
			return queue.poll();
		}

		@Override
		public int available() throws IOException {
			return queue.size();
		}

	}

	public LaunchConfigurationStreamProvider(ILaunchConfiguration launchConfig, Set<String> launchModes) {
		super();
		Assert.isNotNull(launchConfig);
		this.launchConfiguration = launchConfig;
		if (launchModes != null) {
			this.launchModes = launchModes;
		} else {
			this.launchModes = Collections.singleton(ILaunchManager.RUN_MODE);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof LaunchConfigurationStreamProvider)) {
			return false;
		}
		LaunchConfigurationStreamProvider other = (LaunchConfigurationStreamProvider)obj;
		return this.launchConfiguration.equals(other.launchConfiguration) && this.launchModes.equals(other.launchModes);
	}

	@Override
	public int hashCode() {
		return this.launchConfiguration.hashCode() ^ this.launchModes.hashCode();
	}

	public static ILaunchConfiguration findLaunchConfiguration(String typeId, String name) {
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = manager.getLaunchConfigurationType(typeId);
		ILaunchConfiguration res = null;
		try {
			for (ILaunchConfiguration launch : manager.getLaunchConfigurations(type)) {
				if (launch.getName().equals(name)) {
					res = launch;
				}
			}
		} catch (CoreException e) {
			LanguageServerPlugin.logError(e);
		}
		return res;
	}

	@Override
	public void start() throws IOException {
		try {
			launch = this.launchConfiguration.launch(this.launchModes.iterator().next(), new NullProgressMonitor());
		} catch (Exception e) {
			LanguageServerPlugin.logError(e);
		}
	}

	@Override
	public InputStream getInputStream() {
		if (this.inputStream == null) {
			process = this.launch.getProcesses()[0];
			this.inputStream = new StreamProxyInputStream(process);
			process.getStreamsProxy().getOutputStreamMonitor().addListener(this.inputStream);
		}
		return this.inputStream;
	}

	@Override
	public OutputStream getOutputStream() {
		if (this.outputStream == null) {
			try {
				// TODO: Ugly hack, find something better to retrieve stream!
				Method systemProcessGetter = RuntimeProcess.class.getDeclaredMethod("getSystemProcess"); //$NON-NLS-1$
				systemProcessGetter.setAccessible(true);
				Process systemProcess = (Process)systemProcessGetter.invoke(process);
				this.outputStream = systemProcess.getOutputStream();
			} catch (ReflectiveOperationException ex) {
				LanguageServerPlugin.logError(ex);
			}
		}
		return this.outputStream;
	}

	@Override
	public InputStream getErrorStream() {
		if (this.errorStream == null) {
			process = this.launch.getProcesses()[0];
			this.errorStream = new StreamProxyInputStream(process);
			process.getStreamsProxy().getErrorStreamMonitor().addListener(this.errorStream);
		}
		return this.errorStream;
	}

	@Override
	public void stop() {
		if (this.launch == null) {
			return;
		}
		try {
			this.launch.terminate();
			for (IProcess p : this.launch.getProcesses()) {
				p.terminate();
			}
		} catch (DebugException e1) {
			LanguageServerPlugin.logError(e1);
		}
		this.launch = null;
		this.process = null;
		try {
			if (this.inputStream != null) {
				this.inputStream.close();
			}
		} catch (IOException e) {
			LanguageServerPlugin.logError(e);
		}
		try {
			if (this.errorStream != null) {
				this.errorStream.close();
			}
		} catch (IOException e) {
			LanguageServerPlugin.logError(e);
		}
		this.inputStream = null;
		this.outputStream = null;
		this.errorStream = null;
	}

	@Override
	public void notifyContentTypeDisabled(IContentType contentType) {
	}

	@Override
	public boolean enable() {
		return true;
	}

}
