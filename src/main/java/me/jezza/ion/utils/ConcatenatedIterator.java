/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.jezza.ion.utils;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * @author Guava : https://github.com/google/guava/blob/master/guava/src/com/google/common/collect/Iterators.java#L577
 */
public class ConcatenatedIterator<T> implements Iterator<T> {
	/* The last iterator to return an element.  Calls to remove() go to this iterator. */
	private Iterator<? extends T> toRemove;

	/* The iterator currently returning elements. */
	private Iterator<? extends T> iterator;

	/*
	 * We track the "meta iterators," the iterators-of-iterators, below.  Usually, topMetaIterator
	 * is the only one in use, but if we encounter nested concatenations, we start a deque of
	 * meta-iterators rather than letting the nesting get arbitrarily deep.  This keeps each
	 * operation O(1).
	 */
	private Iterator<? extends Iterator<? extends T>> topMetaIterator;

	// Only becomes nonnull if we encounter nested concatenations.
	private Deque<Iterator<? extends Iterator<? extends T>>> metaIterators;

	private ConcatenatedIterator(Iterator<? extends Iterator<? extends T>> metaIterator) {
		iterator = Collections.emptyIterator();
		topMetaIterator = Objects.requireNonNull(metaIterator);
	}

	public static <T> Iterator<T> concat(Iterator<? extends Iterator<? extends T>> inputs) {
		return new ConcatenatedIterator<>(inputs);
	}

	// Returns a nonempty meta-iterator or, if all meta-iterators are empty, null.
	private Iterator<? extends Iterator<? extends T>> getTopMetaIterator() {
		while (topMetaIterator == null || !topMetaIterator.hasNext()) {
			if (metaIterators != null && !metaIterators.isEmpty()) {
				topMetaIterator = metaIterators.removeFirst();
			} else {
				return null;
			}
		}
		return topMetaIterator;
	}

	@Override
	public boolean hasNext() {
		while (!iterator.hasNext()) {
			// this weird checkNotNull positioning appears required by our tests, which expect
			// both hasNext and next to throw NPE if an input iterator is null.

			topMetaIterator = getTopMetaIterator();
			if (topMetaIterator == null) {
				return false;
			}

			iterator = topMetaIterator.next();

			if (iterator instanceof ConcatenatedIterator) {
				// Instead of taking linear time in the number of nested concatenations, unpack
				// them into the queue
				@SuppressWarnings("unchecked")
				ConcatenatedIterator<T> topConcat = (ConcatenatedIterator<T>) iterator;
				iterator = topConcat.iterator;

				// topConcat.topMetaIterator, then topConcat.metaIterators, then this.topMetaIterator,
				// then this.metaIterators

				if (this.metaIterators == null) {
					this.metaIterators = new ArrayDeque<>();
				}
				this.metaIterators.addFirst(this.topMetaIterator);
				if (topConcat.metaIterators != null) {
					while (!topConcat.metaIterators.isEmpty()) {
						this.metaIterators.addFirst(topConcat.metaIterators.removeLast());
					}
				}
				this.topMetaIterator = topConcat.topMetaIterator;
			}
		}
		return true;
	}

	@Override
	public T next() {
		if (hasNext()) {
			toRemove = iterator;
			return iterator.next();
		} else {
			throw new NoSuchElementException();
		}
	}

	@Override
	public void remove() {
		if (toRemove == null)
			throw new IllegalStateException("no calls to next() since the last call to remove()");
		toRemove.remove();
		toRemove = null;
	}
}
