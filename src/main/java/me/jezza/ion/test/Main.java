package me.jezza.ion.test;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import io.netty.channel.ChannelFuture;
import me.jezza.ion.Ion;
import me.jezza.ion.bus.interfaces.Subscriber;

/**
 * @author Jezza
 */
public class Main {
	private static final Ion CLUSTER = Ion.cluster("main-cluster", 5000);

	public static void main(String[] args) throws IOException {
		CLUSTER.local().register(Main.class);

		System.out.println("Posting events");
		List<ChannelFuture> futures = new ArrayList<>();
		for (int i = 0; i < 1000; i++) {
			futures.add(CLUSTER.post(new MyEvent(Integer.toString(i))));
		}
		System.out.println("Done");
		for (ChannelFuture future : futures) {
			future.syncUninterruptibly();
		}
	}

	@Subscriber
	private static void onEvent(MyEvent event) {
		System.out.println("Received custom event: " + event.value);
	}

	public static final class MyEvent implements Serializable {
		public final String value;

		public MyEvent(String value) {
			this.value = value;
		}


		@Override
		public String toString() {
			return "MyEvent{" +
					"value='" + value + '\'' +
					'}';
		}
	}
}
