/**********************************************************************
 * Copyright (c) 2003 IBM Corporation and others. All rights reserved.   This
 * program and the accompanying materials are made available under the terms of
 * the Common Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.core.runtime.jobs;

/**
 * A lock is used to control access to an exclusive resource.
 * <p>
 * Locks are reentrant.  That is, they can be acquired multiple times by the same thread
 * without releasing.  Locks are only released when the number of successful acquires 
 * equals the number of successful releases.
 * </p>
 * <p>
 * Locks avoid circular waiting deadlocks by ensuring that locks
 * are always acquired in a strict order.  This makes it impossible for n such 
 * locks to deadlock while waiting for each other.  This means
 * that during an interval when a process owns a lock, it can be forced
 * to give the lock up and wait until all locks it requires become
 * available.  As a result, the thread owning the lock does not have exclusive access 
 * to the resource for the duration between acquire() and release() calls.
 * </p>
 * <p>
 * Successive acquire attempts by different threads are queued and serviced on
 * a first come, first served basis.
 * </p>
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * @see IJobManager.newLock
 * @since 3.0
 */
public interface ILock {
	/**
	 * Acquires this lock.  If the lock is in use, callers will block until the lock becomes
	 * available.  If this thread owns several locks, callers will be blocked until all
	 * threads they require become available.
	 */
	public void acquire() throws InterruptedException;
	/**
	 * Releases this lock.  Callers must not release locks they do not currently own.
	 */
	public void release();
}