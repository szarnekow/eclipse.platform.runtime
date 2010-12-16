/*******************************************************************************
 *  Copyright (c) 2010 BestSolution.at and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      Tom Schind<tom.schindl@bestsolution.at> - initial API and implementation
 ******************************************************************************/
package org.eclipse.e4.core.internal.services;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import org.eclipse.e4.core.services.translation.ITranslationProvider;
import org.eclipse.e4.core.services.translation.ITranslationService;
import org.osgi.framework.Bundle;

public class TranslationServiceImpl implements ITranslationService {
	private Map<String, ITranslationProvider> providerMap = Collections
			.synchronizedMap(new HashMap<String, ITranslationProvider>());

	public TranslationServiceImpl() {
		System.err.println("Service is created!!!");
	}

	private ITranslationProvider getProvider(String providerId) {
		ITranslationProvider pv = providerMap.get(providerId);
		if (pv == null) {
			pv = tryCreateBundleLocalization(providerId);
			providerMap.put(providerId, pv);
		}

		return pv;
	}

	private ITranslationProvider tryCreateBundleLocalization(String providerId) {
		for (Bundle b : ServicesActivator.getDefault().getContext().getBundles()) {
			String[] parts = providerId.split("@");
			if (b.getSymbolicName().equals(parts[0])
					&& (parts.length == 1 || parts[1].equals(b.getVersion().toString()))) {
				final Bundle bundle = b;
				return new ITranslationProvider() {

					public String translate(String locale, String key) {
						ResourceBundle resBundle = ServicesActivator.getDefault()
								.getBundleLocalization().getLocalization(bundle, locale);
						if (resBundle != null) {
							try {
								return resBundle.getString(key);
							} catch (Exception e) {
								e.printStackTrace();
								return key;
							}
						}
						return key;
					}
				};
			}
		}

		return null;
	}

	public String translate(String locale, String providerId, String key) {
		ITranslationProvider pv = getProvider(providerId);

		if (pv != null) {
			return pv.translate(locale, key);
		}

		return key;
	}

	public String[] translate(String locale, String providerId, String... keys) {
		ITranslationProvider pv = getProvider(providerId);
		if (pv != null) {
			String[] rv = new String[keys.length];
			for (int i = 0; i < keys.length; i++) {
				rv[i] = pv.translate(locale, keys[i]);
			}
			return rv;
		}
		return keys;
	}

	public void registerTranslationProvider(String providerId, ITranslationProvider provider) {
		if (providerMap.containsKey(providerId)) {
			throw new IllegalArgumentException(
					"Can not register more than one provider for category '" + providerId + "'."); //$NON-NLS-1$//$NON-NLS-2$
		}
		providerMap.put(providerId, provider);
	}

	public boolean unregisterTranslationProvider(String providerId) {
		return providerMap.remove(providerId) != null;
	}

	public void dsRegisterTranslationProvider(ITranslationProvider provider,
			Map<String, String> data) {
		try {
			registerTranslationProvider(data.get("PROVIDER_ID"), provider);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

	public void dsUnregisterTranslationProvider(ITranslationProvider provider,
			Map<String, String> data) {
		unregisterTranslationProvider(data.get("PROVIDER_ID"));
	}
}