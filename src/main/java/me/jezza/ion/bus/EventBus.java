package me.jezza.ion.bus;

import java.util.Iterator;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.jezza.ion.bus.interfaces.EventDispatcher;
import me.jezza.ion.bus.interfaces.EventSubscriber;
import me.jezza.ion.bus.interfaces.ExceptionHandler;
import me.jezza.ion.bus.interfaces.Subscriber;
import me.jezza.ion.bus.interfaces.SubscriberRegistry;
import me.jezza.ion.bus.interfaces.SubscriberStrategy;
import me.jezza.ion.bus.interfaces.ThreadSafe;

/**
 * This is an extended version from guava, called the {@link EventBus}.
 * This version explictly adds a lot more control over everything, including selecting subscribers.
 * It uses modularised and compartmentalised code, so any single part of it can be swapped out for a custom implementation.
 * <p>
 * <h2>{@link ExceptionHandler}:</h2> This interface is used to report any issues that events had while being dispatched to any given subscriber.
 * It is given the exception that was thrown along with an {@link ExceptionContext} object that contains the {@link EventBus} that the event was posted on, the event object itself that caused the exception, and the {@link EventSubscriber} that the exception originated from.
 * <p>The default implementation, {@link DefaultExceptionHandler}, simply logs out the error, making sure that it's noticed.
 * <p>
 * <h2>{@link EventDispatcher}:</h2> This interface is used to dispatch any event that was posted. It'll receive the {@link EventBus} the event was posted on, along with the event itself, and the subscribers that should receive the event.
 * If an event failed to fire, or an exception was thrown, the {@link EventDispatcher} should collect the necessary information and pass it back to {@link EventBus#handleException(Throwable, Object, EventSubscriber)}, as it'll construct the appropriate objects, and pass it off to the {@link ExceptionHandler}.
 * The default implementation of {@link EventBus} will post a {@link DeadEvent} if no subscriber was found for any given event, for more information look towards the bottom of this javadoc.
 * This can be overridden as {@link EventBus} is not final.
 * <p>The default implementation, {@link DefaultEventDispatcher}, is a simple, basic, thread-safe dispatcher. It will post the events to every subscriber on the same thread that posted the event, unless another thread posted something first.
 * (Side note here: it will only dispatch on the current thread, if the {@link DefaultEventDispatcher} wasn't already being dispatched by another thread. If another thread posted an event first, and then discovered that it should be dispatched, it'll start the dispatching process, but if another thread comes along while the other thread is dispatching the queue, it'll just append it to the queue, so the first thread that started the dispatch would be the one to dispatch the event that was just posted.)
 * So, subscribers should be fast. If you have to query a database, or something else that could take a while, it's recommended to place the event on a queue, and have another thread do the work.
 * That being said, there's nothing to stop an async implementation of the {@link EventDispatcher}.
 * <p>
 * <h2>{@link SubscriberStrategy}:</h2> This interface is used to locate and return all "subscribers" for a given target.
 * These are commonly methods, but they could be anything the {@link SubscriberStrategy} deems fit. It only has to adher to a simple restriction of it must return {@link EventSubscriber}s, other than that, it's free to do as it wishes.
 * <p>The default implementation, {@link DefaultSubscriberStrategy}, extends off of {@link AnnotatedSubscriberStrategy}, which is a generic class used to locate methods that use certain annotations.
 * That class could be used if you wish to implement your own annotation.
 * <p>
 * <h2>{@link SubscriberRegistry}:</h2> This is an interface that is used for communication
 * with a data structure. {@link #register(Object)} and {@link #unregister(Object)} are forward directly to the {@link SubscriberRegistry}, with the additional parameter of the {@link SubscriberStrategy}.
 * The {@link SubscriberRegistry} should use the {@link SubscriberStrategy} to locate all {@link EventSubscriber}s from a given listener object, and store them for later retrieval.
 * <p>The default implementation, {@link DefaultSubscriberRegistry}, stores all of this data for easy and fast retrieval.
 *
 * <p>
 * <h2>Original Javadoc from Guava's EventBus.</h2>
 * <p>--------------------------------------
 * <p>
 *
 * Dispatches events to listeners, and provides ways for listeners to register themselves.
 *
 * <p>The EventBus allows publish-subscribe-style communication between
 * components without requiring the components to explicitly register with one
 * another (and thus be aware of each other).  It is designed exclusively to
 * replace traditional Java in-process event distribution using explicit
 * registration. It is <em>not</em> a general-purpose publish-subscribe system,
 * nor is it intended for interprocess communication.
 *
 * <h2>Receiving Events</h2>
 * <p>To receive events, an object should:
 * <ol>
 * <li>Expose a public method, known as the <i>event subscriber</i>, which accepts
 *     a single argument of the type of event desired;</li>
 * <li>Mark it with an annotation (By default, it's {@link Subscriber});</li>
 * <li>Pass itself to an EventBus instance's {@link #register(Object)} method.
 *     </li>
 * </ol>
 *
 * <h2>Posting Events</h2>
 * <p>To post an event, simply provide the event object to the
 * {@link #post(Object)} method.  The EventBus instance will determine the type
 * of event and route it to all registered listeners.
 *
 * <p>Events are routed based on their type &mdash; an event will be delivered
 * to any subscriber for any type to which the event is <em>assignable.</em>  This
 * includes implemented interfaces, all superclasses, and all interfaces
 * implemented by superclasses.
 *
 * <p>When {@code #post(Object)} is called, all registered subscribers for an event are run
 * in sequence, so subscribers should be reasonably quick.  If an event may trigger
 * an extended process (such as a database load), spawn a thread or queue it for
 * later.
 *
 * <h2>Subscriber Methods</h2>
 * <p>Event subscriber methods must accept only one argument: the event.
 *
 * <p>Subscribers should not, in general, throw.  If they do, the EventBus will
 * catch and log the exception.  This is rarely the right solution for error
 * handling and should not be relied upon; it is intended solely to help find
 * problems during development.
 *
 * <p>The EventBus guarantees that it will not call a subscriber method from
 * multiple threads simultaneously, unless the method explicitly allows it by
 * bearing the a concurrent annotation (By default this is: {@link ThreadSafe}).
 * If this annotation is not present, subscriber methods need not worry about
 * being reentrant, unless also called from outside the EventBus.
 *
 * <h2>Dead Events</h2>
 * <p>If an event is posted, but no registered subscribers can accept it, it is
 * considered "dead."  To give the system a second chance to handle dead events,
 * they are wrapped in an instance of {@link DeadEvent} and reposted.
 *
 * <p>If a subscriber for a supertype of all events (such as Object) is registered,
 * no event will ever be considered dead, and no DeadEvents will be generated.
 * Accordingly, while DeadEvent extends {@link Object}, a subscriber registered to
 * receive any Object will never receive a DeadEvent.
 *
 * <p>This class is safe for concurrent use.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/EventBusExplained">
 * {@code EventBus}</a>.
 *
 * @author jezza
 * @date 12 Sep 2016
 */
public class EventBus {
	private static final Logger log = LoggerFactory.getLogger(EventBus.class);

	public final String identifier;

	/**
	 * The handler that is called upon whenever an unexpected exception occurs while dispatching events.
	 */
	protected final ExceptionHandler handler;

	/**
	 * A registry that combines finding subscribers within a listener, and keeping track of them.
	 */
	protected final EventDispatcher dispatcher;

	/**
	 * The strategy that will be used when locating subscribers on any given listener object.
	 */
	protected final SubscriberStrategy strategy;

	/**
	 * A registry that combines finding subscribers within a listener, and keeping track of them.
	 */
	protected final SubscriberRegistry registry;

	/**
	 * Constructs a default {@link EventBus} with the default identifier ("default").
	 * It uses the default {@link ExceptionHandler}, {@link EventDispatcher}, {@link SubscriberStrategy}, and {@link SubscriberRegistry}.
	 */
	public EventBus() {
		this("default");
	}

	/**
	 * Constructs an {@link EventBus} with the given identifier.
	 * It uses the default {@link ExceptionHandler}, {@link EventDispatcher}, {@link SubscriberStrategy}, and {@link SubscriberRegistry}.
	 */
	public EventBus(final String identifier) {
		this(identifier, new DefaultExceptionHandler());
	}

	/**
	 * Constructs an {@link EventBus} with the given identifier, and {@link ExceptionHandler}.
	 * It uses the default {@link EventDispatcher}, {@link SubscriberStrategy}, and {@link SubscriberRegistry}.
	 */
	public EventBus(final String identifier, final ExceptionHandler handler) {
		this(identifier, handler, new DefaultEventDispatcher());
	}

	/**
	 * Constructs an {@link EventBus} with the given identifier, {@link ExceptionHandler}, and {@link EventDispatcher}.
	 * It uses the default {@link SubscriberStrategy}, and {@link SubscriberRegistry}.
	 */
	public EventBus(final String identifier, final ExceptionHandler handler, final EventDispatcher dispatcher) {
		this(identifier, handler, dispatcher, new DefaultSubscriberStrategy());
	}

	/**
	 * Constructs an {@link EventBus} with the given identifier, {@link ExceptionHandler}, {@link EventDispatcher}, and {@link SubscriberStrategy}.
	 * It uses the default {@link SubscriberRegistry}.
	 */
	public EventBus(final String identifier, final ExceptionHandler handler, final EventDispatcher dispatcher, final SubscriberStrategy strategy) {
		this(identifier, handler, dispatcher, strategy, new DefaultSubscriberRegistry());
	}

	/**
	 * Constructs an {@link EventBus} with the given identifier, {@link ExceptionHandler}, {@link EventDispatcher}, {@link SubscriberStrategy}, and {@link SubscriberRegistry}.
	 */
	public EventBus(final String identifier, final ExceptionHandler handler, final EventDispatcher dispatcher, final SubscriberStrategy strategy, final SubscriberRegistry registry) {
		this.identifier = Objects.requireNonNull(identifier, "Argument 'identifier' may not be null.");
		this.handler = Objects.requireNonNull(handler, "Argument 'handler' may not be null.");
		this.dispatcher = Objects.requireNonNull(dispatcher, "Argument 'dispatcher' may not be null.");
		this.strategy = Objects.requireNonNull(strategy, "Argument 'strategy' may not be null.");
		this.registry = Objects.requireNonNull(registry, "Argument 'registry' may not be null.");
	}

	/**
	 * Registers all {@link EventSubscriber}s that are discovered on the given listener.
	 * {@link EventSubscriber}s of any given listener are classified as such by the {@link EventBus}'s {@link SubscriberStrategy}.
	 *
	 * @param listener - object whose subscriber methods should be registered.
	 */
	public final void register(final Object listener) {
		registry.register(strategy, listener);
	}

	/**
	 * Unregisters all subscriber methods on a registered {@code object}.
	 *
	 * @param listener - object whose subscriber methods should be unregistered.
	 * @throws IllegalArgumentException if the object was not previously registered.
	 */
	public final void unregister(final Object listener) {
		registry.unregister(strategy, listener);
	}

	/**
	 * Posts an event to all registered subscribers.  This method will return
	 * successfully after the event has been posted to all subscribers, and
	 * regardless of any exceptions thrown by subscribers.
	 *
	 * <p>If no subscribers have been subscribed for {@code event}'s class, and
	 * {@code event} is not already a {@link DeadEvent}, it will be wrapped in a
	 * DeadEvent and reposted.
	 *
	 * @param event - event to post.
	 */
	public void post(final Object event) {
		final Iterator<EventSubscriber> subscribers = registry.subscribersFor(event);
		if (subscribers != null && subscribers.hasNext()) {
			dispatcher.dispatch(this, event, subscribers);
		} else if (!(event instanceof DeadEvent)) {
			post(new DeadEvent(this, event));
		}
	}

	/**
	 * Handles the given exception thrown by a subscriber with the given event.
	 * Used to construct the context, and pass that through the {@link ExceptionHandler}.
	 *
	 * @param exception  - The exception that was thrown.
	 * @param event      - The event the subscriber threw the exception on.
	 * @param subscriber - The subscriber that threw the exception.
	 */
	public void handleException(final Throwable exception, final Object event, final EventSubscriber subscriber) {
		Objects.requireNonNull(exception, "Argument 'exception' may not be null.");
		Objects.requireNonNull(event, "Argument 'event' may not be null.");
		Objects.requireNonNull(subscriber, "Argument 'subscriber' may not be null.");
		try {
			handler.handleException(exception, new ExceptionContext(this, event, subscriber));
		} catch (final Throwable e) {
			// If the exception handler throws, log it. There isn't much else to do!
			log.error(String.format("Exception %s thrown while handling exception: %s", e, exception.getCause()), e);
		}
	}

	@Override
	public String toString() {
		return "[EventBus-" + identifier + ']';
	}
}
