package me.jezza.ion.bus;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import me.jezza.ion.bus.interfaces.EventSubscriber;
import me.jezza.ion.bus.interfaces.SubscriberStrategy;
import me.jezza.ion.bus.subscriber.MethodEventSubscriber;
import me.jezza.ion.bus.subscriber.SynchronisedMethodEventSubscriber;
import me.jezza.ion.utils.Types;

/**
 * A base implementation of finding {@link EventSubscriber}s from methods that have a certain annotation on them.
 * The annotations in question are passed into the constructor.
 *
 * @author jezza
 * @date 12 Sep 2016
 */
public class AnnotatedSubscriberStrategy implements SubscriberStrategy {

	/**
	 * A thread-safe cache that contains the mapping from each class to all methods in that class and
	 * all super-classes, that are annotated with {@code @Subscribe}. The cache is shared across all
	 * instances of this class; this greatly improves performance if multiple EventBus instances are
	 * created and objects of the same class are registered on all of them.
	 */
	protected final LoadingCache<Class<?>, Collection<Method>> subscriberMethodCache = Caffeine.newBuilder()
			.weakKeys()
			.build(new CacheLoader<Class<?>, Collection<Method>>() {
				@Override
				public Collection<Method> load(final Class<?> listenerClass) throws Exception {
					return collectAnnotatedMethods(listenerClass, subscriber);
				}
			});

	protected final Class<? extends Annotation> subscriber;
	protected final Class<? extends Annotation> concurrent;

	public AnnotatedSubscriberStrategy(final Class<? extends Annotation> subscriber, final Class<? extends Annotation> concurrent) {
		this.subscriber = Objects.requireNonNull(subscriber, "Argument 'subscriber' may not be null.");
		this.concurrent = Objects.requireNonNull(concurrent, "Argument 'concurrent' may not be null.");
	}

	protected final Collection<Method> fromMethodCache(final Class<?> listenerClass) {
		return subscriberMethodCache.get(listenerClass);
	}

	@Override
	public Map<Class<?>, Collection<EventSubscriber>> findAllSubscribers(final Object listener) {
		final Collection<Method> methods = fromMethodCache(classOf(listener));
		final Map<Class<?>, Collection<EventSubscriber>> methodsInListener = new HashMap<>();
		for (final Method method : methods) {
			final EventSubscriber subscriber = makeSubscriber(listener, method);
			if (subscriber != null) {
				// We check the parameter length within getAnnotatedMethods, so this shouldn't break.
				methodsInListener.computeIfAbsent(method.getParameterTypes()[0], k -> new ArrayList<>()).add(subscriber);
			}
		}
		return methodsInListener;
	}

	protected EventSubscriber makeSubscriber(Object listener, final Method method) {
		final boolean isClass = listener instanceof Class;
		// If it's a static dispatch, we only care if it's a static method, similarly, if it's a virtual dispatch, we only care about virtual methods.
		if (isClass ^ Modifier.isStatic(method.getModifiers())) {
			return null;
		}
		if (isClass) {
			listener = null;
		}
		return threadSafe(method)
				? new MethodEventSubscriber(listener, method)
				: new SynchronisedMethodEventSubscriber(listener, method);
	}

	protected boolean threadSafe(final Method method) {
		return method.getAnnotation(concurrent) != null;
	}

	@SuppressWarnings("unchecked")
	protected Class<?> classOf(final Object listener) {
		return listener instanceof Class ? (Class<?>) listener : listener.getClass();
	}

	protected Collection<Method> collectAnnotatedMethods(final Class<?> listenerClass, final Class<? extends Annotation> annotationClass) {
		final Set<? extends Class<?>> types = Types.classes(listenerClass);

		final Map<MethodIdentifier, Method> identifiers = new HashMap<>();
		for (final Class<?> superClazz : types) {
			//	for (final Method method : superClazz.getMethods()) {
			for (final Method method : superClazz.getDeclaredMethods()) {
				if (method.isAnnotationPresent(annotationClass) && !method.isBridge()) {
					// We might have to do a bit more here, as generics jump to the upper bound, which means it will never be the argument that you wanted.
					final int parameterCount = method.getParameterCount();
					if (parameterCount != 1) {
						final String message = String.format("Method '%s' has @%s annotation, but requires %s arguments.  Event subscriber methods must require a single argument.",
								method,
								annotationClass.getSimpleName(),
								Integer.toString(parameterCount));
						throw new IllegalArgumentException(message);
					}

					final MethodIdentifier ident = new MethodIdentifier(method);
					if (!identifiers.containsKey(ident)) {
						identifiers.put(ident, method);
					}
				}
			}
		}
		return identifiers.values();
	}

	public static final class MethodIdentifier {
		private final String name;
		private final Class<?>[] parameterTypes;

		public MethodIdentifier(final Method method) {
			name = method.getName().intern();
			parameterTypes = method.getParameterTypes();
		}

		@Override
		public int hashCode() {
			int result = 31 + (name == null ? 0 : name.hashCode());
			for (final Object element : parameterTypes) {
				result = 31 * result + (element == null ? 0 : element.hashCode());
			}
			return result;
		}

		@Override
		public boolean equals(final Object o) {
			if (o instanceof MethodIdentifier) {
				final MethodIdentifier ident = (MethodIdentifier) o;
				return name.equals(ident.name) && Arrays.equals(parameterTypes, ident.parameterTypes);
			}
			return false;
		}

		@Override
		public String toString() {
			return name + '(' + Arrays.stream(parameterTypes).map(Class::getName).collect(Collectors.joining(", ")) + ')';
		}
	}
}
