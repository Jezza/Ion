package me.jezza.ion.utils;

import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;

import sun.misc.Unsafe;

/**
 * @author Jezza
 */
public final class Bypass {
	public static final Unsafe UNSAFE;
	public static final Lookup LOOKUP;

	static {
		try {
			Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
			unsafe.setAccessible(true);
			UNSAFE = (Unsafe) unsafe.get(null);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to locate Unsafe.", e);
		}


		try {
			Field lookup = Lookup.class.getDeclaredField("IMPL_LOOKUP");
			lookup.setAccessible(true);
			LOOKUP = (Lookup) lookup.get(null);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to locate the trusted Lookup.", e);
		}
	}

	private Bypass() {
		throw new IllegalStateException();
	}
}
