package me.jezza.ion.bus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import me.jezza.ion.bus.interfaces.EventSubscriber;
import me.jezza.ion.bus.interfaces.SubscriberRegistry;
import me.jezza.ion.bus.interfaces.SubscriberStrategy;
import me.jezza.ion.utils.ConcatenatedIterator;
import me.jezza.ion.utils.Types;

/**
 * @author jezza
 * @date 12 Sep 2016
 */
public class DefaultSubscriberRegistry implements SubscriberRegistry {

	private static final CacheLoader<Class<?>, Set<Class<?>>> CACHE_LOADER = concreteClass -> Collections.unmodifiableSet(Types.classes(concreteClass));

	/**
	 * A thread-safe cache for flattenHierarchy().
	 * The Class class is immutable.
	 * This cache is shared across all EventBus instances, which greatly improves performance if multiple such instances
	 * are created and objects of the same class are posted on all of them.
	 */
	private static final LoadingCache<Class<?>, Set<Class<?>>> FLATTEN_HIERARCHY_CACHE = Caffeine.newBuilder()
			.weakKeys()
			.build(CACHE_LOADER);

	/**
	 * All registered subscribers, indexed by event type.
	 * <p>
	 * <p>The {@link CopyOnWriteArraySet} values make it easy and relatively lightweight to get an
	 * immutable snapshot of all current subscribers to an event without any locking.
	 */
	private final ConcurrentMap<Class<?>, CopyOnWriteArraySet<EventSubscriber>> subscribers = new ConcurrentHashMap<>();

	public DefaultSubscriberRegistry() {
	}

	@Override
	public void register(final SubscriberStrategy strategy, final Object listener) {
		final Map<Class<?>, Collection<EventSubscriber>> listenerMethods = strategy.findAllSubscribers(listener);
		for (final Map.Entry<Class<?>, Collection<EventSubscriber>> entry : listenerMethods.entrySet()) {
			// Get the subscriber set for the given event type (Create the set if it's not there), and add all of the new subscribers.
			subscribers.computeIfAbsent(entry.getKey(), k -> new CopyOnWriteArraySet<>()).addAll(entry.getValue());
		}
	}

	@Override
	public void unregister(final SubscriberStrategy strategy, final Object listener) {
		final Map<Class<?>, Collection<EventSubscriber>> listenerMethods = strategy.findAllSubscribers(listener);
		for (final Map.Entry<Class<?>, Collection<EventSubscriber>> entry : listenerMethods.entrySet()) {
			final CopyOnWriteArraySet<EventSubscriber> currentSubscribers = subscribers.get(entry.getKey());
			if (currentSubscribers == null || !currentSubscribers.removeAll(entry.getValue())) {
				// if removeAll returns true, all we really know is that at least one subscriber was
				// removed... however, barring something very strange we can assume that if at least one
				// subscriber was removed, all subscribers on listener for that event type were removed...
				throw new IllegalArgumentException("Missing event subscriber for an annotated method. Is " + listener + " registered?");
			}
			// don't try to remove the set if it's empty; that can't be done safely without a lock
			// anyway, if the set is empty it'll just be wrapping an array of length 0
		}
	}

	@Override
	public Iterator<EventSubscriber> subscribersFor(final Object event) {
		Objects.requireNonNull(event, "Argument 'event' may not be null.");
		final Set<Class<?>> eventTypes = flattenHierarchy(event.getClass());
		final List<Iterator<EventSubscriber>> subscriberIterators = new ArrayList<>(eventTypes.size());
		for (final Class<?> eventType : eventTypes) {
			final CopyOnWriteArraySet<EventSubscriber> eventSubscribers = subscribers.get(eventType);
			if (eventSubscribers != null) {
				// eager no-copy snapshot
				subscriberIterators.add(eventSubscribers.iterator());
			}
		}
		return subscriberIterators.isEmpty()
				? Collections.emptyIterator()
				: ConcatenatedIterator.concat(subscriberIterators.iterator());
	}

	/**
	 * Flattens a class's type hierarchy into a set of Class objects.  The set
	 * will include all superclasses (transitively), and all interfaces
	 * implemented by these superclasses.
	 *
	 * @param concreteClass
	 * 		class whose type hierarchy will be retrieved.
	 * @return {@code clazz}'s complete type hierarchy, flattened and uniqued.
	 */
	protected static final Set<Class<?>> flattenHierarchy(final Class<?> concreteClass) {
		return FLATTEN_HIERARCHY_CACHE.get(concreteClass);
	}
}
