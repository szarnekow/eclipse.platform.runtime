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
package org.eclipse.e4.core.services.translation;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Provides translation for a given category
 */
public interface ITranslationProvider {
	/**
	 * <p>
	 * Translate the give key in the given locale's text.
	 * </p>
	 * The lookup of the translation the same than the one in
	 * {@link ResourceBundle#getBundle(String, Locale, ClassLoader)}
	 * <ul>
	 * <li>Ls + "_" + Cs + "_" + Vs</li>
	 * <li>Ls + "_" + Cs</li>
	 * <li>Ls</li>
	 * <li>Ld + "_" + Cd + "_" + Vd</li>
	 * <li>Ld + "_" + Cd</li>
	 * <li>Ld</li>
	 * <li>DEFAULT</li>
	 * </ul>
	 * Where Ls, Cs and Vs are the specified locale (language, country, variant) and Ld, Cd and Vd
	 * are the default locale (language, country, variant).
	 * 
	 * @param locale
	 *            the locale, must not be <code>null</code>
	 * @param category
	 *            the category
	 * @param key
	 *            the key
	 * @return the translated value must not be <code>null</code>
	 * @throws MissingResourceException
	 *             if translation fails because of whatever reason
	 */
	public String translate(Locale locale, String category, String key)
			throws MissingResourceException;
}
