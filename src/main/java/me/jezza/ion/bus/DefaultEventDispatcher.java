package me.jezza.ion.bus;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

import me.jezza.ion.bus.interfaces.EventDispatcher;
import me.jezza.ion.bus.interfaces.EventSubscriber;

/**
 * @author jezza
 * @date 12 Sep 2016
 */
public class DefaultEventDispatcher implements EventDispatcher {
	/** queues of events for the current thread to dispatch */
	protected final ThreadLocal<Queue<Event>> queue = ThreadLocal.withInitial(ArrayDeque::new);

	/** true if the current thread is currently dispatching an event */
	protected final ThreadLocal<Boolean> dispatching = ThreadLocal.withInitial(() -> Boolean.FALSE);

	public DefaultEventDispatcher() {
	}

	@Override
	public void dispatch(final EventBus bus, final Object event, final Iterator<EventSubscriber> subscribers) {
		// Queue the {@code event} for dispatch during the inner dispatch.
		// Events are queued in-order of occurrence so they can be dispatched in the same order.
		final Queue<Event> queueForThread = queue.get();
		queueForThread.offer(new Event(event, subscribers));
		if (!dispatching.get().booleanValue()) {
			dispatching.set(Boolean.TRUE);
			// Drain the queue of events to be dispatched. As the queue is being drained, new events may be posted to the end of the queue.
			try {
				Event nextEvent;
				while ((nextEvent = queueForThread.poll()) != null) {
					while (nextEvent.subscribers.hasNext()) {
						final EventSubscriber subscriber = nextEvent.subscribers.next();
						try {
							subscriber.handle(nextEvent.event);
						} catch (final Throwable e) {
							bus.handleException(e, event, subscriber);
						}
					}
				}
			} finally {
				dispatching.remove();
				queue.remove();
			}
		}
	}

	/**
	 * A simple struct representing an event and its subscribers.
	 *
	 * @author jezza
	 * @date 12 Sep 2016
	 */
	private static final class Event {
		final Object event;
		final Iterator<EventSubscriber> subscribers;

		Event(final Object event, final Iterator<EventSubscriber> subscribers) {
			this.event = event;
			this.subscribers = subscribers;
		}
	}
}
