package me.jezza.ion.utils;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.function.Predicate;

/**
 * @author Jezza
 */
public final class Addresses {
	private Addresses() {
		throw new IllegalStateException();
	}

	public static Pair<NetworkInterface, InetAddress> getIp4Address() throws SocketException {
		return getIpAddress(address -> address instanceof Inet4Address);
	}

	public static Pair<NetworkInterface, InetAddress> getIp6Address() throws SocketException {
		return getIpAddress(address -> address instanceof Inet6Address);
	}

	public static Pair<NetworkInterface, InetAddress> getIpAddress(Predicate<InetAddress> condition) throws SocketException {
		// Before we connect somewhere, we cannot be sure about what we'd be bound to; however,
		// we only connect when the message where client ID is, is long constructed. Thus,
		// just use whichever IP address we can find.
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		while (interfaces.hasMoreElements()) {
			NetworkInterface current = interfaces.nextElement();
			if (!current.isUp() || current.isLoopback() || current.isVirtual())
				continue;
			Enumeration<InetAddress> addresses = current.getInetAddresses();
			while (addresses.hasMoreElements()) {
				InetAddress address = addresses.nextElement();
				if (address.isLoopbackAddress())
					continue;
				if (condition.test(address)) {
					return Pair.of(current, address);
				}
			}
		}
		throw new SocketException("Can't get our ip address, interfaces are: " + interfaces);
	}
}
