package org.eclipse.core.internal.locks;

import org.eclipse.core.internal.runtime.Assert;

/**
 * A lock used to control write access to an exclusive resource.
 * 
 * The lock uses a circular wait algorithm to ensure that locks
 * are always acquired in a strict order.  This makes it impossible
 * for n such locks to deadlock.  The downside is that this means
 * that during an interval when a process owns a lock, it can be forced
 * to give the lock up and wait until all locks it requires become
 * available.  This removes the feature of exclusive access to the
 * resource in contention for the duration between acquire() and
 * release() calls.
 * 
 * This lock is reentrant.  The same process can acquire the lock
 * any number of times.
 * 
 * The lock implementation prevents starvation by granting the
 * lock in the same order in which acquire() requests arrive. In
 * this scheme, starvation is only possible if a thread retains
 * a lock indefinitely.
 */
public class Lock {
	private static final boolean DEBUG = true;
	/**
	 * Locks are sequentially ordered for debugging purposes.
	 */
	private static int nextLockNumber = 0;
	/**
	 * The thread of the operation that currently owns the lock.
	 */
	private Thread currentOperationThread;
	/**
	 * Records the number of successive acquires in the same
	 * thread. The thread is released only when the depth
	 * reaches zero.
	 */
	private int depth = 0;
	/**
	 * The manager that implements the circular wait protocol.
	 */
	private final LockManager manager;
	private final int number;
	/**
	 * Queue of semaphores for operations currently waiting
	 * on the lock.
	 */
	private final Queue operations = new Queue();

	/**
	 * Returns a new workspace lock.
	 */
	protected Lock(LockManager manager) {
		this.manager = manager;
		this.number = nextLockNumber++;
	}
	/**
	 * Acquires this lock.  Callers will block indefinitely
	 * until this lock becomes available.
	 */
	public void acquire() throws InterruptedException {
		Lock[] toAcquire = manager.computeLocksToAcquire(this);
		//acquire the necessary locks in ascending order
		for (int i = 0; i < toAcquire.length; i++) {
			toAcquire[i].doAcquire();
		}
	}
	/**
	 * Returns null if acquired and a Semaphore object otherwise.
	 */
	private synchronized Semaphore acquireSemaphore() {
		if (isCurrentOperation())
			return null;
		//if nobody is waiting, grant the lock immediately
		if (currentOperationThread == null && operations.isEmpty()) {
			currentOperationThread = Thread.currentThread();
			return null;
		}
		return enqueue(new Semaphore(Thread.currentThread()));
	}
	/**
	 * Attempts to acquire this lock.  Callers will block indefinitely 
	 * until this lock comes available to them.  
	 * <p>
	 * Clients may extend this method but should not otherwise call it.
	 * </p>
	 * @see #release
	 */
	protected void doAcquire() throws InterruptedException {
		Semaphore semaphore = acquireSemaphore();
		if (semaphore != null) {
			if (DEBUG)
				System.out.println("[" + Thread.currentThread() + "] Operation waiting to be executed... :-/"); //$NON-NLS-1$ //$NON-NLS-2$
			try {
				semaphore.acquire();
			} catch (InterruptedException e) {
				if (DEBUG)
					System.out.println("[" + Thread.currentThread() + "] Operation interrupted while waiting... :-|"); //$NON-NLS-1$ //$NON-NLS-2$
				throw e;
			}
			updateCurrentOperation();
			if (DEBUG)
				System.out.println("[" + Thread.currentThread() + "] Operation started... :-)"); //$NON-NLS-1$ //$NON-NLS-2$
			Assert.isTrue(depth == 0, "Lock depth is invalid"); //$NON-NLS-1$
		}
		depth++;
	}
	/**
	 * If there is another semaphore with the same runnable in the
	 * queue, the other is returned and the new one is not added.
	 */
	private synchronized Semaphore enqueue(Semaphore newSemaphore) {
		Semaphore semaphore = (Semaphore) operations.get(newSemaphore);
		if (semaphore == null) {
			operations.enqueue(newSemaphore);
			return newSemaphore;
		}
		return semaphore;
	}
	/**
	 * Returns the thread of the current operation, or null if
	 * there is no current operation
	 */
	protected synchronized Thread getCurrentOperationThread() {
		return currentOperationThread;
	}
	private synchronized boolean isCurrentOperation() {
		return currentOperationThread == Thread.currentThread();
	}
	/**
	 * Releases this lock allowing others to acquire it.
	 * @see #acquire
	 */
	public synchronized void release() {
		Assert.isTrue(currentOperationThread == Thread.currentThread(), "Lock released by wrong thread"); //$NON-NLS-1$
		//only release the lock when the depth reaches zero
		if (--depth == 0) {
			Semaphore next = (Semaphore) operations.peek();
			currentOperationThread = null;
			if (next != null)
				next.release();
		}
	}
	/**
	 * For debugging purposes only.
	 */
	public String toString() {
		return "Lock(" + number + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	/**
	 * Removes the waiting operation from the queue
	 * and updates the current operation thread.
	 */
	private synchronized void updateCurrentOperation() {
		operations.dequeue();
		currentOperationThread = Thread.currentThread();
	}
}