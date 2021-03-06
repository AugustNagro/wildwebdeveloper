/*******************************************************************************
 * Copyright (c) 2018, 2019 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Mickael Istria (Red Hat Inc.) - initial implementation
 *   Pierre-Yves B. - Issue #180 Wrong path to nodeDebug.js
 *******************************************************************************/
package org.eclipse.wildwebdeveloper.debug.chrome;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wildwebdeveloper.Activator;
import org.eclipse.wildwebdeveloper.InitializeLaunchConfigurations;
import org.eclipse.wildwebdeveloper.debug.AbstractHTMLDebugDelegate;

import com.google.gson.JsonObject;

public class ChromeRunDAPDebugDelegate extends AbstractHTMLDebugDelegate {
	
	static final String ID = "org.eclipse.wildwebdeveloper.launchConfiguration.chromeRunDebug"; //$NON-NLS-1$

	static final String VERBOSE = "verbose";
	private static final String TRACE = "trace";
	private static final String RUNTIME_EXECUTABLE = "runtimeExecutable";
	private static final String SOURCE_MAPS = "sourceMaps";

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {
		// user settings
		Map<String, Object> param = new HashMap<>();
		
		// File to debug 
		param.put("file", configuration.getAttribute(AbstractHTMLDebugDelegate.PROGRAM, "no program path defined")); //$NON-NLS-1$
		
		// Chrome executable arguments
		String argsString = configuration.getAttribute(AbstractHTMLDebugDelegate.ARGUMENTS, "").trim(); //$NON-NLS-1$
		if (!argsString.isEmpty()) {
			Object[] args = Arrays.asList(argsString.split(" ")).stream() //$NON-NLS-1$
					.filter(s -> !s.trim().isEmpty()).toArray();
			if (args.length > 0) {
				param.put(AbstractHTMLDebugDelegate.ARGUMENTS, args);
			}
		}

		// Debug environment variables
		Map<String, String> env = configuration.getAttribute(AbstractHTMLDebugDelegate.ENV,
				Collections.emptyMap());
		if (!env.isEmpty()) {
			JsonObject envJson = new JsonObject();
			env.forEach((key, value) -> envJson.addProperty(key, value));
			param.put(AbstractHTMLDebugDelegate.ENV, envJson);
		}
		
		// Chrome working directory
		String cwd = configuration.getAttribute(AbstractHTMLDebugDelegate.CWD, "").trim(); //$NON-NLS-1$
		if (!cwd.isEmpty()) {
			param.put(AbstractHTMLDebugDelegate.CWD, cwd);
		}

		param.put(SOURCE_MAPS, true);
		
		// TODO: Let user point to the location of their Chrome executable
		param.put(RUNTIME_EXECUTABLE, findChromeLocation());
		
		if (configuration.getAttribute(VERBOSE, false)) {
			param.put(TRACE, VERBOSE);
		}

		super.launchWithParameters(configuration, mode, launch, monitor, param, findDebugAdapter());
	}
	
	static File findDebugAdapter() {
		URL fileURL;
		try {
			fileURL = FileLocator.toFileURL(
					ChromeRunDAPDebugDelegate.class.getResource("/language-servers/node_modules/debugger-for-chrome/out/src/chromeDebug.js"));
			return new File(fileURL.toURI());
		} catch (IOException | URISyntaxException e) {
			IStatus errorStatus = new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
			Activator.getDefault().getLog().log(errorStatus);
			Display.getDefault().asyncExec(() -> ErrorDialog.openError(Display.getDefault().getActiveShell(), "Debug error", e.getMessage(), errorStatus)); //$NON-NLS-1$
		}
		return null;
		
	}

	private String findChromeLocation() {
		String res = InitializeLaunchConfigurations.which("chromium-browser");
		if (res == null) {
			res = InitializeLaunchConfigurations.which("google-chrome-stable");
		}
		if (res == null) {
			res = "path/to/chrome";
		}
		return res;
	}

}
