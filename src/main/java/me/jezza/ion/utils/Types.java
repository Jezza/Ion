package me.jezza.ion.utils;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author Jezza
 */
public final class Types {
	private Types() {
		throw new IllegalStateException();
	}

	public static <T> Set<Class<? super T>> classes(Class<T> start) {
		Objects.requireNonNull(start);
		Set<Class<? super T>> types = new LinkedHashSet<>();
		Class<? super T> type = start;
		while (type != null) {
			types.add(type);
			Class<?>[] interfaces = type.getInterfaces();
			if (interfaces != null && interfaces.length > 0) {
				for (Class<?> interfaceType : interfaces) {
					@SuppressWarnings("unchecked")
					Class<? super T> typeCheck = (Class<? super T>) interfaceType;
					types.add(typeCheck);
				}
			}
			type = type.getSuperclass();
		}
		return types;
	}
}
