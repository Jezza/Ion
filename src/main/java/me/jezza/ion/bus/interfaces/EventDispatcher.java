package me.jezza.ion.bus.interfaces;

import java.util.Iterator;

import me.jezza.ion.bus.EventBus;

/**
 * @author jezza
 * @date 12 Sep 2016
 */
public interface EventDispatcher {
	void dispatch(EventBus bus, Object event, Iterator<EventSubscriber> subscribers);
}
