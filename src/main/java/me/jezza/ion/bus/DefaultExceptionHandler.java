package me.jezza.ion.bus;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.jezza.ion.bus.interfaces.ExceptionHandler;

/**
 * @author jezza
 * @date 12 Sep 2016
 */
public class DefaultExceptionHandler implements ExceptionHandler {
	private static final Logger log = LoggerFactory.getLogger(EventBus.class);

	@Override
	public void handleException(final Throwable exception, final ExceptionContext context) {
		log.error("Could not dispatch event (" + context.event.getClass() + "): " + context.subscriber, exception);
	}
}
