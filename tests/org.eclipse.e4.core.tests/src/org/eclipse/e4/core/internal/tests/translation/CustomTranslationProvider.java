package org.eclipse.e4.core.internal.tests.translation;

import org.eclipse.e4.core.services.translation.PropertiesBundleTranslationProvider;

public class CustomTranslationProvider extends
		PropertiesBundleTranslationProvider {
	public CustomTranslationProvider() {
		super(CustomTranslationProvider.class.getClassLoader(),
				"OSGI-INF/l10n/messages");

	}
}
