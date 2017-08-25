package me.jezza.ion.test;

import java.io.IOException;

import me.jezza.ion.Ion;
import me.jezza.ion.bus.interfaces.Subscriber;
import me.jezza.ion.bus.interfaces.ThreadSafe;
import me.jezza.ion.test.Main.MyEvent;

/**
 * @author Jezza
 */
public class Listener {
	private static final Ion CLUSTER = Ion.cluster("main-cluster", 5000);

	static {
		CLUSTER.local().register(Listener.class);
	}

	public static void main(String[] args) throws IOException {
		System.out.println("Starting....");
		System.in.read();
		System.out.println("Exiting...");
	}

	@Subscriber
	@ThreadSafe
	public static void incoming(MyEvent event) {
		System.out.println("Received event: " + event.value);
	}
}
