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

/**
 * An item in a linked list
 */
public class ListEntry {
	private ListEntry next;
	/**
	 * Adds an entry at the end of the list of which this item is the head.
	 */
	protected final void addLast(ListEntry entry) {
		if (next == null) {
			next = entry;
			entry.next = null;
		} else
			next.addLast(entry);
	}
	/**
	 * Adds an entry immediately after this one in the list.
	 */
	protected final void insertNext(ListEntry entry) {
		entry.next = this.next;
		this.next = entry;
	}
	/**
	 * Returns the next entry in the list, or null if there is no next entry
	 */
	protected final ListEntry next() {
		return next;
	}
	protected final void setNext(ListEntry entry) {
		this.next = entry;
	}
}