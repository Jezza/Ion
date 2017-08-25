package me.jezza.ion.bus.subscriber;

import java.lang.reflect.Method;

/**
 * @author jezza
 * @date 12 Sep 2016
 */
public class SynchronisedMethodEventSubscriber extends MethodEventSubscriber {

	public SynchronisedMethodEventSubscriber(final Object target, final Method method) {
		super(target, method);
	}

	@Override
	public void handle(final Object event) throws Throwable {
		synchronized (this) {
			super.handle(event);
		}
	}
}
