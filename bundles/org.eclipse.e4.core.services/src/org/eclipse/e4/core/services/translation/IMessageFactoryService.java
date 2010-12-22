package org.eclipse.e4.core.services.translation;

public interface IMessageFactoryService {
	public <M> M createInstance(final String locale, final Class<M> messages)
			throws InstantiationException, IllegalAccessException;
}
