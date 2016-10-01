package me.jezza.ion.cluster;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Basically a config object.
 * It does the connection handling.
 *
 * @author Jezza
 */
public final class IonClusterBuilder {
	private static final String LOWER_KEY = "ion.cluster.range.lower";
	private static final String UPPER_KEY = "ion.cluster.range.upper";

	private static final String LOWER_DEFAULT = "224.0.0.1";
	private static final String UPPER_DEFAULT = "239.255.255.255";

	private static final byte[] LOWER;
	private static final byte[] UPPER;
	private static final byte[] RANGE;

	static {
		try {
			String lower = System.getProperty(LOWER_KEY);
			InetAddress lowerBound = InetAddress.getByName(lower == null ? LOWER_DEFAULT : lower);
			if (!lowerBound.isMulticastAddress())
				throw new IllegalArgumentException("Invalid lower bound: " + lowerBound);

			String upper = System.getProperty(UPPER_KEY);
			InetAddress upperBound = InetAddress.getByName(upper == null ? UPPER_DEFAULT : upper);
			if (!upperBound.isMulticastAddress())
				throw new IllegalArgumentException("Invalid upper bound: " + upperBound);

			LOWER = lowerBound.getAddress();
			UPPER = upperBound.getAddress();
			int l = UPPER.length;
			byte[] result = new byte[l];

			byte up;
			byte low;
			for (int i = 0; i < l; i++) {
				up = UPPER[i];
				low = LOWER[i];
				result[i] = (up & 0xFF) > (low & 0xFF) ? (byte) (up - low) : 0;
			}
			if (assertArray(result))
				throw new IllegalArgumentException("Lower and Upper IP Bounds are invalid. The lower bound should be below the upper bound.");
			RANGE = result;
		} catch (UnknownHostException e) {
			throw new IllegalStateException(e);
		}
	}

	private static boolean assertArray(final byte[] array) {
		for (byte b : array)
			if (b != 0)
				return false;
		return true;
	}

	private final String name;
	private final InetAddress normalised;

	public IonClusterBuilder(String name) {
		this.name = name;
		normalised = normalise(name);
		System.out.println(normalised);
	}

	private InetAddress normalise(String name) {
		byte[] range = RANGE;
		int l = range.length;
		byte[] bits = hash(name, l);
		byte[] lower = LOWER;
		byte[] result = new byte[l];
		for (int i = l - 1; i >= 0; i--) {
			byte r = range[i];
			byte bit = bits[i];
			if (r == 0) {
				result[i] = lower[i];
			} else if ((bit & 0xFF) > (r & 0xFF)) {
				result[i] = (byte) ((bit & 0xFF) % r + lower[i]);
			} else {
				result[i] = bit;
			}
		}
		try {
			return InetAddress.getByAddress(result);
		} catch (UnknownHostException e) {
			throw new IllegalStateException("Normalisation failed: '" + name + "', Range:" + Arrays.toString(LOWER) + ',' + Arrays.toString(UPPER));
		}
	}

	private byte[] hash(String name, int length) {
		return ByteBuffer.allocate(length).putInt(name.hashCode()).array();
	}

	public IonCluster join(int port) {
		return join(port, null);
	}

	public IonCluster join(int port, NetworkInterface _net) {
		try {
			InetSocketAddress local = new InetSocketAddress(port);
			MulticastSocket socket = new MulticastSocket(local);
			InetSocketAddress group = new InetSocketAddress(normalised, port);
			socket.joinGroup(group, _net);
			NetworkInterface net = socket.getNetworkInterface();
			return new IonCluster(name, local, group, net, socket);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public String toString() {
		return "[IonClusterBuilder:" + name + ':' + normalised + ']';
	}
}
