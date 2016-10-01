package me.jezza.ion.cluster;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;

/**
 * @author Jezza
 */
public final class IonCluster {
	private final String name;
	private final InetSocketAddress local;
	private final InetSocketAddress group;
	private final NetworkInterface net;
	private final MulticastSocket socket;

	IonCluster(String name, InetSocketAddress local, InetSocketAddress group, NetworkInterface net, MulticastSocket socket) {
		this.name = name;
		this.local = local;
		this.group = group;
		this.net = net;
		this.socket = socket;
	}

	public void sendData(String data) throws IOException {
		DatagramPacket packet = new DatagramPacket(data.getBytes(StandardCharsets.UTF_8), 0, data.length(), group);
		socket.send(packet);
	}

	public void sendObject(Object object) throws IOException {
	}

	public void poll(DatagramPacket data) throws IOException {
		socket.receive(data);
	}

	@Override
	public String toString() {
		return "[IonCluster:" + name + ']';
	}

}
