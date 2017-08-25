package me.jezza.ion.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @author Jezza
 */
public final class Strings {

	private static final String LOWER_KEY = "ion.cluster.range.lower";
	private static final String UPPER_KEY = "ion.cluster.range.upper";

	private static final String LOWER_DEFAULT = "224.0.0.1";
	private static final String UPPER_DEFAULT = "239.255.255.255";

	private static final byte[] LOWER;
	private static final byte[] UPPER;
	private static final byte[] RANGE;

	static {
		try {
			String lower = System.getProperty(LOWER_KEY, LOWER_DEFAULT);
			InetAddress lowerBound = InetAddress.getByName(lower);
			if (!lowerBound.isMulticastAddress())
				throw new IllegalArgumentException("Invalid lower bound: " + lowerBound);

			String upper = System.getProperty(UPPER_KEY, UPPER_DEFAULT);
			InetAddress upperBound = InetAddress.getByName(upper);
			if (!upperBound.isMulticastAddress())
				throw new IllegalArgumentException("Invalid upper bound: " + upperBound);

			LOWER = lowerBound.getAddress();
			UPPER = upperBound.getAddress();
			int l = UPPER.length;
			byte[] result = new byte[l];

			int up;
			int low;
			for (int i = 0; i < l; i++) {
				up = UPPER[i] & 0xFF;
				low = LOWER[i] & 0xFF;
				result[i] = up > low
						? (byte) (up - low)
						: 0;
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

	private Strings() {
		throw new IllegalStateException();
	}

	public static InetAddress normalise(String name) {
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
			} else if ((bit & 0xFF) >= (r & 0xFF)) {
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

	private static byte[] hash(String name, int length) {
		return ByteBuffer.allocate(length)
				.putInt(name.hashCode())
				.array();
	}
}
