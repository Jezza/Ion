package me.jezza.ion.bus.interfaces;

import me.jezza.ion.bus.ExceptionContext;

/**
 * @author jezza
 * @date 12 Sep 2016
 */
public interface ExceptionHandler {
	void handleException(Throwable exception, ExceptionContext context);
}
