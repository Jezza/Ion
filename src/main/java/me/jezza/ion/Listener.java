package me.jezza.ion;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.UnknownHostException;

import me.jezza.ion.cluster.IonCluster;

/**
 * Just a test class.
 *
 * @author Jezza
 */
public class Listener {
	public static void main(String[] args) throws UnknownHostException {
		IonCluster cluster = Ion.cluster("myUniqueNetwork").join(6457);

		try {
			String msg = "Here's some new data!";
			cluster.sendData(msg);
			byte[] buf = new byte[10 * 1024];
			DatagramPacket data = new DatagramPacket(buf, buf.length);
			while (true) {
				cluster.poll(data);
				System.out.println("Received: " + new String(buf, 0, data.getLength()));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
