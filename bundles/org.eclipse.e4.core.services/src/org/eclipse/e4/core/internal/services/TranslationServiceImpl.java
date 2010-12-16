package org.eclipse.e4.core.internal.services;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.eclipse.e4.core.services.translation.ITranslationProvider;
import org.eclipse.e4.core.services.translation.ITranslationService;
import org.eclipse.osgi.service.localization.LocaleProvider;
import org.osgi.framework.Bundle;

public class TranslationServiceImpl implements ITranslationService {
	private Map<String, ITranslationProvider> providerMap = Collections
			.synchronizedMap(new HashMap<String, ITranslationProvider>());

	public TranslationServiceImpl() {
		System.err.println("Service is created!!!");
	}

	private Locale getLocale() {
		LocaleProvider localeProvider = ServicesActivator.getDefault().getLocaleProvider();
		if (localeProvider != null) {
			Locale currentLocale = localeProvider.getLocale();
			if (currentLocale != null)
				return currentLocale;
		}
		return Locale.getDefault();
	}

	public String translate(String category, String key) {
		return translate(getLocale(), category, key);
	}

	public String translate(Locale locale, String category, String key) {
		ITranslationProvider pv = getProvider(category);

		if (pv != null) {
			return pv.translate(locale, category, key);
		}

		throw new MissingResourceException("Category '" + category + " is not known'", null, key);
	}

	private ITranslationProvider getProvider(String category) {
		ITranslationProvider pv = providerMap.get(category);
		if (pv == null) {
			pv = tryCreateBundleLocalization(category);
			// TODO Should we cache it? Then we need to add a tracker if the bundle is
			// installed/uninstalled
		}

		return pv;
	}

	private ITranslationProvider tryCreateBundleLocalization(String category) {
		for (Bundle b : ServicesActivator.getDefault().getContext().getBundles()) {
			String[] parts = category.split("@");
			if (b.getSymbolicName().equals(parts[0])
					&& (parts.length == 1 || parts[1].equals(b.getVersion().toString()))) {
				final Bundle bundle = b;
				return new ITranslationProvider() {

					public String translate(Locale locale, String category, String key) {
						ResourceBundle resBundle = ServicesActivator.getDefault()
								.getBundleLocalization().getLocalization(bundle, locale.toString());
						if (resBundle != null) {
							try {
								return resBundle.getString(key);
							} catch (Exception e) {
								throw new MissingResourceException("Could not find '" + key
										+ "' in bundle localization.", resBundle.getClass()
										.getName(), key);
							}
						}
						throw new MissingResourceException("Bundle '" + category
								+ " has no localization informations.'", null, key);
					}
				};
			}
		}

		return null;
	}

	public String[] translate(String category, String... keys) {
		return translate(getLocale(), category, keys);
	}

	public String[] translate(Locale locale, String category, String... keys) {
		ITranslationProvider pv = getProvider(category);
		if (pv != null) {
			String[] rv = new String[keys.length];
			for (int i = 0; i < keys.length; i++) {
				rv[i] = pv.translate(locale, category, keys[i]);
			}
			return rv;
		}
		throw new MissingResourceException("Category '" + category + " is not known'", null, null);
	}

	public void registerTranslationProvider(String category, ITranslationProvider provider) {
		if (providerMap.containsKey(category)) {
			throw new IllegalArgumentException(
					"Can not register more than one provider for category '" + category + "'."); //$NON-NLS-1$//$NON-NLS-2$
		}
		providerMap.put(category, provider);
	}

	public boolean unregisterTranslationProvider(String category, ITranslationProvider provider) {
		if (providerMap.get(category) == provider) {
			providerMap.remove(category);
			return true;
		}
		return false;
	}

	public void dsRegisterTranslationProvider(ITranslationProvider provider,
			Map<String, String> data) {
		for (String s : data.get("CATEGORIES").split(";")) { //$NON-NLS-1$//$NON-NLS-2$
			try {
				registerTranslationProvider(s, provider);
			} catch (IllegalArgumentException e) {
				// TODO: handle exception
			}
		}
	}

	public void dsUnregisterTranslationProvider(ITranslationProvider provider,
			Map<String, String> data) {
		for (String s : data.get("CATEGORIES").split(";")) { //$NON-NLS-1$//$NON-NLS-2$
			unregisterTranslationProvider(s, provider);
		}
	}
}