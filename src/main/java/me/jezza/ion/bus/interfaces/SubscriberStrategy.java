package me.jezza.ion.bus.interfaces;

import java.util.Collection;
import java.util.Map;

/**
 * @author jezza
 * @date 12 Sep 2016
 */
public interface SubscriberStrategy {
	/**
	 * Discovers all {@link EventSubscriber}s for a given listener.
	 *
	 * @param listener - The listener to search.
	 * @return A {@link Map} of event classes mapped to a collection of {@link EventSubscriber}s.
	 */
	Map<Class<?>, Collection<EventSubscriber>> findAllSubscribers(Object listener);
}
