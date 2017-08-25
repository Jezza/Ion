package me.jezza.ion.bus.interfaces;

import java.util.Iterator;

/**
 * @author jezza
 * @date 12 Sep 2016
 */
public interface SubscriberRegistry {

	/**
	 * Registers the given listener, allowing the {@link SubscriberRegistry} to create and store any {@link EventSubscriber}s that the {@link SubscriberRegistry} represents.
	 *
	 * @param strategy - The strategy that should be used to retreive any {@link EventSubscriber}s from the given listener.
	 * @param listener - The object that was registered.
	 */
	void register(SubscriberStrategy strategy, Object listener);

	/**
	 * Unregisters any and all {@link EventSubscriber}s that originated from this listener.
	 *
	 * @param strategy - The strategy that should be used to retreive any {@link EventSubscriber}s from the given listener.
	 * @param listener - The object that should be unregistered.
	 */
	void unregister(SubscriberStrategy strategy, Object listener);

	/**
	 * An iterator all of {@link EventSubscriber}s for a given event.
	 *
	 * @param event - The event that was posted.
	 * @return - An immutable iterator of all {@link EventSubscriber}s that accept the given event. Note: doesn't have to be immutable, but it's recommended.
	 */
	Iterator<EventSubscriber> subscribersFor(Object event);
}
