package me.jezza.ion.bus.subscriber;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

import me.jezza.ion.bus.interfaces.EventSubscriber;

/**
 * @author jezza
 * @date 12 Sep 2016
 */
public class MethodEventSubscriber implements EventSubscriber {
	/**
	 * The object that contains the method, or if the method is static, null.
	 */
	private final Object target;
	/**
	 * The subscriber method.
	 */
	private final Method method;

	public MethodEventSubscriber(final Object target, final Method method) {
		this.method = Objects.requireNonNull(method, "Argument 'method' may not be null.");
		// If the method is static, we don't care about the target.
		this.target = Modifier.isStatic(method.getModifiers()) ? target : Objects.requireNonNull(target, "Argument 'target' may not be null.");
		method.setAccessible(true);
	}

	@Override
	public void handle(final Object event) throws Throwable {
		Objects.requireNonNull(event, "Argument 'event' may not be null.");
		try {
			method.invoke(target, event);
		} catch (final IllegalArgumentException e) {
			throw new Error("Method rejected target/argument: " + event, e);
		} catch (final IllegalAccessException e) {
			throw new Error("Method became inaccessible: " + event, e);
		} catch (final InvocationTargetException e) {
			if (e.getCause() instanceof Error) {
				throw e.getCause();
			}
			throw e;
		}
	}

	@Override
	public String toString() {
		return "[EventSubscriber:" + method + "]";
	}

	@Override
	public int hashCode() {
		return (31 + method.hashCode()) * 31 + System.identityHashCode(target);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof MethodEventSubscriber) {
			final MethodEventSubscriber that = (MethodEventSubscriber) obj;
			// Use == so that different equal instances will still receive events.
			// We only guard against the case that the same object is registered
			// multiple times
			return target == that.target && method.equals(that.method);
		}
		return false;
	}
}
