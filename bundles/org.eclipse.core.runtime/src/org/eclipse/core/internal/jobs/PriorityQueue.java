/**********************************************************************
 * Copyright (c) 2003 IBM Corporation and others. All rights reserved.   This
 * program and the accompanying materials are made available under the terms of
 * the Common Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.core.internal.jobs;

import java.util.Comparator;

/**
 * A heap-based priority queue.  The class currently uses a standard array-based heap
 * Either the elements in the queue must implement Comparable, or a Comparator
 * must be provided.
 * 
 * This heap sorts itself lazily because elements can change their priority at any time.
 * Insertion is done in constant time, and removal takes O(nlg(n)) time.
 */
public class PriorityQueue {
	private final Comparator comparator;
	private Object[] elements;
	//	number of elements in the queue
	private int size = 0;

	/**
	 * Create a queue with the given capacity.  Using this constructor assumes the
	 * objects in the queue implement the <code>java.lang.Comparable</code>
	 * interface
	 */
	public PriorityQueue(int capacity) {
		this(capacity, null);
	}
	/**
	 * Create a queue with the given initial capacity and comparator
	 */
	public PriorityQueue(int capacity, Comparator comparator) {
		if (capacity <= 0)
			capacity = 1;
		elements = new Object[capacity];
		this.comparator = comparator;
	}
	/** 
	 * remove all elements 
	 */
	public void clear() {
		for (int i = 0; i < size; ++i)
			elements[i] = null;
		size = 0;
	}
	/** 
	 * perform element comparisons
	 */
	protected int compare(Object a, Object b) {
		if (comparator == null)
			return ((Comparable) a).compareTo(b);
		else
			return comparator.compare(a, b);
	}
	/**
	 * Returns true if the given element is in the queue, and false otherwise. 
	 * 
	 * NOTE: Containment is based on identity, not equality.
	 */
	public boolean contains(Object object) {
		return indexOf(object) >= 0;
	}
	/**
	 * Return and remove the element with highest priority, or
	 * null if empty.
	 */
	public Object dequeue() {
		if (size < 1)
			return null;
		final Object max = elements[0];
		//swap the last item in the heap to the root, and heapify from the root
		elements[0] = elements[--size];
		elements[size] = null;
		pushDown(0);
		return max;
	}
	/**
	 * Adds an item to the queue, growing as necessary
	 */
	public void enqueue(Object object) {
		if (size >= elements.length) {
			int newSize = 3 * elements.length / 2 + 1;
			Object[] newElements = new Object[newSize];
			System.arraycopy(elements, 0, newElements, 0, elements.length);
			elements = newElements;
		}
		int i = size++;
		elements[i] = object;
		pullUp(i);
	}
	/**
	 * Returns the index of the given object in the queue, or -1 if the object is
	 * not found.
	 */
	private int indexOf(Object object) {
		for (int i = elements.length; --i >= 0;) {
			if (elements[i] == object)
				return i;
		}
		return -1;
	}

	/**
	 * This element assumes that the heap is valid, except element i, which may
	 * be larger than its parents.  Element i needs to be bubbled up until it finds
	 * its correct place in the heap.
	 */
	private void pullUp(int i) {
		Object object = elements[i];
		while (i > 0) {
			int parent = parent(i);
			//if the object is smaller than its parent, we're done.
			if (compare(object, elements[parent]) < 0)
				break;
			//otherwise move parent down and move to next parent
			elements[i] = elements[parent];
			i = parent;
		}
		elements[i] = object;
	}
	/**
	 * This method assumes that the children of element i are legal heaps, but element
	 * i may be smaller than its children.  Push element i down until the heap is legal.
	 */
	private void pushDown(int i) {
		//resort the remaining elements
		Object current = elements[i];
		for (;;) {
			int l = left(i);
			if (l >= size)
				break;
			else {
				int r = right(i);
				//compare the element with the greatest child
				int child = (r >= size || compare(elements[l], elements[r]) > 0) ? l : r;
				if (compare(current, elements[child]) < 0) {
					elements[i] = elements[child];
					i = child;
				} else
					break;
			}
		}
		elements[i] = current;
	}
	/**
	 * Removes the given element from the heap.  Returns true if the element was
	 * removed, and false otherwise.
	 */
	public boolean remove(Object o) {
		int i = indexOf(o);
		if (i < 0)
			return false;
		//swap the last item in the heap to the deleted index, and bubble down
		elements[i] = elements[--size];
		elements[size] = null;
		pushDown(i);
		return true;
	}
	/**
	 * The given object has changed priority.  Reshuffle the heap until it is valid.
	 */
	public void resort(Object object) {
		//first need to find the element.
		int i = indexOf(object);
		if (i < 0)
			return;
		//if the element is now greater than its parent, pull it up.  Otherwise, push down
		if (i > 0 && compare(elements[i], elements[parent(i)]) > 0)
			pullUp(i);
		else
			pushDown(i);
	}
	/**
	 * Returns true if the queue is empty, and false otherwise.
	 */
	public boolean isEmpty() {
		return size == 0;
	}
	/**
	 * Returns the index of the left child of the element at the given index.
	 */
	protected final int left(int i) {
		return 2 * i + 1;
	}
	/**
	 * Returns the index of the parent element of the element at the given index
	 */
	protected final int parent(int i) {
		return (i - 1) / 2;
	}
	/** 
	 * Return greatest element without removing it, or null if empty 
	 */
	public Object peek() {
		return size > 0 ? elements[0] : null;
	}
	/**
	 * Returns the index of the right child of the element at the given index.
	 */
	protected final int right(int i) {
		return 2 * (i + 1);
	}
	/** 
	 * Return number of elements 
	 */
	public int size() {
		return size;
	}
}