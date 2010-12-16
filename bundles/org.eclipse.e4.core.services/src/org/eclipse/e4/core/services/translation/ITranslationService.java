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
import org.eclipse.osgi.service.localization.BundleLocalization;

/**
 * Service which can be used to translate texts into locale specific ones.
 * 
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
 * Where Ls, Cs and Vs are the specified locale (language, country, variant) and Ld, Cd and Vd are
 * the default locale (language, country, variant).
 * <p>
 * <b>PoviderId:</b>
 * <ul>
 * <li>All bundle symbolic names</li>
 * <li>Providers registered through DI</li>
 * <li>Providers registered through
 * {@link ITranslationService#registerTranslationProvider(String, ITranslationProvider)}</li>
 * </ul>
 * </p>
 */
public interface ITranslationService {
	/**
	 * Translate the key into the default locale.
	 * 
	 * <p>
	 * <b>Category lookup</b>
	 * <ul>
	 * <li>try to find a {@link ITranslationProvider} for the category</li>
	 * <li>interpret the value as a symbolic bundle name and try to access the
	 * {@link BundleLocalization}</li>
	 * </ul>
	 * </p>
	 * 
	 * @param locale
	 *            the locale to use
	 * @param providerId
	 *            the providerId
	 * @param key
	 *            the key to translate
	 * @return the translated value, must not be <code>null</code>
	 * @throws MissingResourceException
	 *             if some problem occurrs while translating
	 */
	public String translate(String locale, String providerId, String key);

	/**
	 * Translate the key into the default locale.
	 * 
	 * <p>
	 * <b>Category lookup</b>
	 * <ul>
	 * <li>try to find a {@link ITranslationProvider} for the category</li>
	 * <li>interpret the value as a symbolic bundle name and try to access the
	 * {@link BundleLocalization}</li>
	 * </ul>
	 * </p>
	 * 
	 * @param locale
	 *            the locale to use
	 * @param providerId
	 *            the providerId
	 * @param key
	 *            the key to translate
	 * @return the translated value, must not be <code>null</code>
	 * @throws MissingResourceException
	 *             if some problem occurrs while translating
	 */
	public String[] translate(String locale, String providerId, String... key);

	/**
	 * Register a translation provider for a category
	 * 
	 * @param category
	 *            the category
	 * @param providerId
	 *            the provider
	 * @throws IllegalArgumentException
	 *             thrown if there's already a provider registered
	 */
	public void registerTranslationProvider(String providerId, ITranslationProvider provider)
			throws IllegalArgumentException;

	/**
	 * Unregister a translation provider for a category
	 * 
	 * @param providerId
	 *            the category of the provider
	 * @param provider
	 *            the provider instance to unregister
	 * @return <code>true</code> if unregistering succeeded
	 */
	public boolean unregisterTranslationProvider(String providerId);
}