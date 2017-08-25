package me.jezza.ion.bus.interfaces;

/**
 * @author jezza
 * @date 12 Sep 2016
 */
@FunctionalInterface
public interface EventSubscriber {
	void handle(Object event) throws Throwable;
}
