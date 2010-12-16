/*******************************************************************************
 *  Copyright (c) 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.core.internal.services;

import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.localization.BundleLocalization;
import org.eclipse.osgi.service.localization.LocaleProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class ServicesActivator implements BundleActivator {

	static private ServicesActivator defaultInstance;
	private BundleContext bundleContext;
	private ServiceTracker debugTracker = null;
	private ServiceTracker localeProviderTracker = null;
	private ServiceTracker bundleLocalizationTracker = null;

	public ServicesActivator() {
		defaultInstance = this;
	}

	public static ServicesActivator getDefault() {
		return defaultInstance;
	}

	public void start(BundleContext context) throws Exception {
		bundleContext = context;
	}

	public void stop(BundleContext context) throws Exception {
		if (debugTracker != null) {
			debugTracker.close();
			debugTracker = null;
		}

		if (localeProviderTracker != null) {
			localeProviderTracker.close();
			localeProviderTracker = null;
		}

		if (bundleLocalizationTracker != null) {
			bundleLocalizationTracker.close();
			bundleLocalizationTracker = null;
		}

		bundleContext = null;
	}

	public boolean getBooleanDebugOption(String option, boolean defaultValue) {
		if (debugTracker == null) {
			debugTracker = new ServiceTracker(bundleContext, DebugOptions.class.getName(), null);
			debugTracker.open();
		}
		DebugOptions options = (DebugOptions) debugTracker.getService();
		if (options != null) {
			String value = options.getOption(option);
			if (value != null)
				return value.equalsIgnoreCase("true"); //$NON-NLS-1$
		}
		return defaultValue;
	}

	public LocaleProvider getLocaleProvider() {
		if (localeProviderTracker == null) {
			localeProviderTracker = new ServiceTracker(bundleContext,
					LocaleProvider.class.getName(), null);
			localeProviderTracker.open();
		}

		return (LocaleProvider) localeProviderTracker.getService();
	}

	public BundleLocalization getBundleLocalization() {
		if (bundleLocalizationTracker == null) {
			bundleLocalizationTracker = new ServiceTracker(bundleContext,
					BundleLocalization.class.getName(), null);
			bundleLocalizationTracker.open();
		}
		return (BundleLocalization) bundleLocalizationTracker.getService();
	}

	public BundleContext getContext() {
		return bundleContext;
	}
}
