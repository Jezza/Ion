package me.jezza.ion.utils;

import java.io.Serializable;

/**
 * A simple compound to hold two values.<br>
 * It is serializable as long as it's values are serializable
 *
 * @author Dirk
 * @date 12.01.2016
 */
public final class Pair<A, B> implements Serializable {
	private static final long serialVersionUID = 1L;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static final Pair EMPTY = new Pair(null, null);

	private final A first;
	private final B second;

	private int hashcode = 0;

	Pair(final A first, final B second) {
		this.first = first;
		this.second = second;
	}

	public final A first() {
		return first;
	}

	public final B second() {
		return second;
	}

	@Override
	public String toString() {
		return "[" + first + ',' + second + ']';
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Pair)) {
			return false;
		}

		final Pair<?, ?> other = (Pair<?, ?>) obj;

		final Object oFirst = other.first;
		if (first != oFirst && (first == null || !first.equals(oFirst))) {
			return false;
		}
		final Object oSecond = other.second;
		if (second != oSecond && (second == null || !second.equals(oSecond))) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		if (hashcode == 0) {
			int hash = 17;
			hash = hash * 37 + (first != null ? first.hashCode() : 0);
			hash = hash * 37 + (second != null ? second.hashCode() : 0);
			hashcode = hash;
		}
		return hashcode;
	}

	public static <A, B> Pair<A, B> of(final A first, final B second) {
		return new Pair<>(first, second);
	}

	public static <A, B> Pair<A, B> empty() {
		return EMPTY;
	}
}