package me.jezza.ion.bus;

import java.util.Objects;

import me.jezza.ion.bus.interfaces.EventSubscriber;

/**
 * @author jezza
 * @date 12 Sep 2016
 */
public final class ExceptionContext {
	/**
	 * The {@link EventBus} that handled the event and the subscriber.
	 * Useful for broadcasting a a new event based on the error.
	 */
	public final EventBus eventBus;

	/**
	 * The event object that caused the subscriber to throw an exception.
	 */
	public final Object event;

	/**
	 * The {@link EventSubscriber} that threw the exception.
	 */
	public final EventSubscriber subscriber;

	/**
	 * @param eventBus 		- The {@link EventBus} that handled the event and the subscriber. Useful for broadcasting a new event based on the error.
	 * @param event 		- The event object that caused the subscriber to throw.
	 * @param subscriber	- The {@link EventSubscriber} that threw the exception.
	 */
	public ExceptionContext(final EventBus eventBus, final Object event, final EventSubscriber subscriber) {
		this.eventBus = Objects.requireNonNull(eventBus, "Argument 'eventBus' may not be null.");
		this.event = Objects.requireNonNull(event, "Argument 'event' may not be null.");
		this.subscriber = Objects.requireNonNull(subscriber, "Argument 'subscriber' may not be null.");
	}
}
