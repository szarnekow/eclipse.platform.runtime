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
 * A linked list based priority queue.  
 * Either the elements in the queue must implement Comparable, or a Comparator
 * must be provided.
 */
public class PriorityQueue {
	private final Comparator comparator;
	// the head of the queue
	private ListEntry head = null;

	/**
	 * Create a new queue.  Using this constructor assumes the
	 * objects in the queue implement the <code>java.lang.Comparable</code>
	 * interface
	 */
	public PriorityQueue() {
		this(null);
	}
	/**
	 * Create a queue with the given comparator
	 */
	public PriorityQueue(Comparator comparator) {
		this.comparator = comparator;
	}
	/** 
	 * remove all elements 
	 */
	public void clear() {
		head = null;
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
		ListEntry entry = head;
		while (entry != null && entry != object)
			entry = entry.next();
		return entry != null;
	}
	/**
	 * Return and remove the element with highest priority, or
	 * null if empty.
	 */
	public ListEntry dequeue() {
		if (head == null)
			return null;
		ListEntry max = head;
		head = head.next();
		max.setNext(null);
		return max;
	}
	/**
	 * Adds an entire list of entries to the queue
	 */
	public void enqueueAll(ListEntry newEntry) {
		while (newEntry != null) {
			ListEntry next = newEntry.next();
			enqueue(newEntry);
			newEntry = next;
		}
	}
	/**
	 * Adds an item to the queue
	 */
	public void enqueue(ListEntry newEntry) {
		if (head == null) {
			head = newEntry;
			newEntry.setNext(null);
			return;
		}
		if (compare(head, newEntry) > 0) {
			newEntry.setNext(head);
			head = newEntry;
			return;
		}
		ListEntry greater = head;
		ListEntry next = greater.next();
		while (next != null && (compare(next, newEntry) <= 0)) {
			greater = next;
			next = greater.next();
		}
		//insert the new entry between greater and next
		greater.insertNext(newEntry);
	}
	/**
	 * Removes the given element from the heap.  Returns true if the element was
	 * removed, and false otherwise.
	 */
	public boolean remove(ListEntry toRemove) {
		if (head == null)
			return false;
		if (toRemove == head) {
			head = head.next();
			toRemove.setNext(null);
			return true;
		}
		ListEntry previous = head;
		ListEntry next = previous.next();
		while (next != null && next != toRemove) {
			previous = next;
			next = next.next();
		}
		if (next != null) {
			previous.setNext(next.next());
			toRemove.setNext(null);
			return true;
		}
		return false;
	}
	/**
	 * The given object has changed priority.  Reshuffle the heap until it is valid.
	 */
	public void resort(ListEntry entry) {
		if (remove(entry))
			enqueue(entry);
		
	}
	/**
	 * Returns true if the queue is empty, and false otherwise.
	 */
	public boolean isEmpty() {
		return head == null;
	}
	/** 
	 * Return greatest element without removing it, or null if empty 
	 */
	public Object peek() {
		return head;
	}
}