package org.eclipse.e4.core.internal.tests.translation;

import java.util.Locale;

import junit.framework.TestCase;

import org.eclipse.e4.core.internal.tests.CoreTestsActivator;
import org.eclipse.e4.core.services.translation.ITranslationService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

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
		String providerId = "org.eclipse.e4.core.tests";
		ITranslationService s = getService();

		assertEquals("Value En",
				s.translate(Locale.ENGLISH.toString(), providerId, "key1"));

		Locale.setDefault(Locale.GERMAN);

		assertEquals("Wert De",
				s.translate(Locale.GERMAN.toString(), providerId, "key1"));

		assertEquals("Wert De", s.translate(Locale.FRENCH.toString(),
				"org.eclipse.e4.core.tests", "key1"));

		assertEquals("Value En",
				s.translate(Locale.ENGLISH.toString(), providerId, "key1"));

		assertEquals("unknown",
				s.translate(Locale.ENGLISH.toString(), providerId, "unknown"));

		assertEquals(2, s.translate(Locale.FRENCH.toString(), providerId,
				"key1", "key2").length);
		assertEquals("Wert De", s.translate(Locale.FRENCH.toString(),
				providerId, "key1", "key2")[0]);
		assertEquals("Wert De 2", s.translate(Locale.FRENCH.toString(),
				providerId, "key1", "key2")[1]);

		assertEquals(2, s.translate(Locale.ENGLISH.toString(), providerId,
				"key1", "key2").length);
		assertEquals("Value En", s.translate(Locale.ENGLISH.toString(),
				providerId, "key1", "key2")[0]);
		assertEquals("Value En 2", s.translate(Locale.ENGLISH.toString(),
				providerId, "key1", "key2")[1]);
	}

	public void testTranslationProvider() {
		String providerId = "org.eclipse.e4.core.tests.customcat";
		ITranslationService s = getService();

		assertEquals("Cust Value En",
				s.translate(Locale.ENGLISH.toString(), providerId, "key1"));

		Locale.setDefault(Locale.GERMAN);

		assertEquals("Cust Wert De",
				s.translate(Locale.GERMAN.toString(), providerId, "key1"));

		assertEquals("Cust Wert De",
				s.translate(Locale.FRENCH.toString(), providerId, "key1"));

		assertEquals("Cust Value En",
				s.translate(Locale.ENGLISH.toString(), providerId, "key1"));

		assertEquals("unknown",
				s.translate(Locale.ENGLISH.toString(), providerId, "unknown"));

		assertEquals(2, s.translate(Locale.FRENCH.toString(), providerId,
				"key1", "key2").length);
		assertEquals("Cust Wert De", s.translate(Locale.FRENCH.toString(),
				providerId, "key1", "key2")[0]);
		assertEquals("Cust Wert De 2", s.translate(Locale.FRENCH.toString(),
				providerId, "key1", "key2")[1]);

		assertEquals(2, s.translate(Locale.ENGLISH.toString(), providerId,
				"key1", "key2").length);
		assertEquals("Cust Value En", s.translate(Locale.ENGLISH.toString(),
				providerId, "key1", "key2")[0]);
		assertEquals("Cust Value En 2", s.translate(Locale.ENGLISH.toString(),
				providerId, "key1", "key2")[1]);
	}
}
