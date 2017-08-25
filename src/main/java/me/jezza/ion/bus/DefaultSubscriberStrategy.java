package me.jezza.ion.bus;

import me.jezza.ion.bus.interfaces.ThreadSafe;
import me.jezza.ion.bus.interfaces.Subscriber;

/**
 * @author jezza
 * @date 12 Sep 2016
 */
public class DefaultSubscriberStrategy extends AnnotatedSubscriberStrategy {
	public DefaultSubscriberStrategy() {
		super(Subscriber.class, ThreadSafe.class);
	}
}
