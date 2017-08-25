package me.jezza.ion.bus;

import java.util.Objects;

/**
 * @author jezza
 * @date 12 Sep 2016
 */
public final class DeadEvent {
	public final EventBus source;
	public final Object event;

	public DeadEvent(final EventBus source, final Object event) {
		this.source = Objects.requireNonNull(source, "Argument 'source' may not be null.");
		this.event = Objects.requireNonNull(event, "Argument 'event' may not be null.");
	}

	@Override
	public String toString() {
		return "Dead{" + source + ':' + event + '}';
	}
}
