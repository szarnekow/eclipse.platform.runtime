package org.eclipse.e4.core.internal.tests.translation;

import java.util.Locale;
import java.util.MissingResourceException;

import org.eclipse.e4.core.internal.tests.CoreTestsActivator;
import org.eclipse.e4.core.services.translation.ITranslationService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import junit.framework.TestCase;

public class TestTranslationService extends TestCase {
	private ITranslationService service;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Locale.setDefault(Locale.ENGLISH);
	}

	public ITranslationService getService() {
		if (service == null) {
			BundleContext ctx = CoreTestsActivator.getDefault()
					.getBundleContext();
			ServiceReference<ITranslationService> ref = ctx
					.getServiceReference(ITranslationService.class);
			service = ctx.getService(ref);
		}

		return service;
	}

	public void testBundleTranslations() {
		String category = "org.eclipse.e4.core.tests";
		ITranslationService s = getService();

		assertEquals("Value En", s.translate(category, "key1"));
		assertEquals("Value En", s.translate(Locale.ENGLISH, category, "key1"));

		Locale.setDefault(Locale.GERMAN);

		assertEquals("Wert De", s.translate(category, "key1"));
		assertEquals("Wert De", s.translate(Locale.GERMAN, category, "key1"));

		assertEquals("Wert De",
				s.translate(Locale.FRENCH, "org.eclipse.e4.core.tests", "key1"));

		assertEquals("Value En", s.translate(Locale.ENGLISH, category, "key1"));

		try {
			s.translate(category, "unknown");
			fail("Unknown key has to fail with MissingResourceException");
		} catch (MissingResourceException e) {
		}

		try {
			s.translate(Locale.ENGLISH, category, "unknown");
			fail("Unknown key has to fail with MissingResourceException");
		} catch (MissingResourceException e) {
		}

		assertEquals(2, s.translate(category, "key1", "key2").length);
		assertEquals("Wert De", s.translate(category, "key1", "key2")[0]);
		assertEquals("Wert De 2", s.translate(category, "key1", "key2")[1]);

		assertEquals(2,
				s.translate(Locale.FRENCH, category, "key1", "key2").length);
		assertEquals("Wert De",
				s.translate(Locale.FRENCH, category, "key1", "key2")[0]);
		assertEquals("Wert De 2",
				s.translate(Locale.FRENCH, category, "key1", "key2")[1]);

		assertEquals(2,
				s.translate(Locale.ENGLISH, category, "key1", "key2").length);
		assertEquals("Value En",
				s.translate(Locale.ENGLISH, category, "key1", "key2")[0]);
		assertEquals("Value En 2",
				s.translate(Locale.ENGLISH, category, "key1", "key2")[1]);
	}

	public void testTranslationProvider() {
		String category = "org.eclipse.e4.core.tests.customcat";
		ITranslationService s = getService();

		assertEquals("Cust Value En", s.translate(category, "key1"));
		assertEquals("Cust Value En",
				s.translate(Locale.ENGLISH, category, "key1"));

		Locale.setDefault(Locale.GERMAN);

		assertEquals("Cust Wert De", s.translate(category, "key1"));
		assertEquals("Cust Wert De",
				s.translate(Locale.GERMAN, category, "key1"));

		assertEquals("Cust Wert De",
				s.translate(Locale.FRENCH, category, "key1"));

		assertEquals("Cust Value En",
				s.translate(Locale.ENGLISH, category, "key1"));

		try {
			s.translate(category, "unknown");
			fail("Unknown key has to fail with MissingResourceException");
		} catch (MissingResourceException e) {
		}

		try {
			s.translate(Locale.ENGLISH, category, "unknown");
			fail("Unknown key has to fail with MissingResourceException");
		} catch (MissingResourceException e) {
		}

		assertEquals(2, s.translate(category, "key1", "key2").length);
		assertEquals("Cust Wert De", s.translate(category, "key1", "key2")[0]);
		assertEquals("Cust Wert De 2", s.translate(category, "key1", "key2")[1]);

		assertEquals(2,
				s.translate(Locale.FRENCH, category, "key1", "key2").length);
		assertEquals("Cust Wert De",
				s.translate(Locale.FRENCH, category, "key1", "key2")[0]);
		assertEquals("Cust Wert De 2",
				s.translate(Locale.FRENCH, category, "key1", "key2")[1]);

		assertEquals(2,
				s.translate(Locale.ENGLISH, category, "key1", "key2").length);
		assertEquals("Cust Value En",
				s.translate(Locale.ENGLISH, category, "key1", "key2")[0]);
		assertEquals("Cust Value En 2",
				s.translate(Locale.ENGLISH, category, "key1", "key2")[1]);
	}
}
