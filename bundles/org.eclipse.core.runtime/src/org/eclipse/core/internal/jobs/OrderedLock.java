package org.eclipse.core.internal.jobs;

import org.eclipse.core.internal.runtime.Assert;
import org.eclipse.core.runtime.jobs.ILock;

/**
 * A lock used to control write access to an exclusive resource.
 * 
 * The lock avoids circular waiting deadlocks by ensuring that locks
 * are always acquired in a strict order.  This makes it impossible for n such 
 * locks to deadlock while waiting for each other.  The downside is that this means
 * that during an interval when a process owns a lock, it can be forced
 * to give the lock up and wait until all locks it requires become
 * available.  This removes the feature of exclusive access to the
 * resource in contention for the duration between acquire() and
 * release() calls.
 * 
 * The lock implementation prevents starvation by granting the
 * lock in the same order in which acquire() requests arrive. In
 * this scheme, starvation is only possible if a thread retains
 * a lock indefinitely.
 */
public class OrderedLock implements ILock {
	private static final boolean DEBUG = true;
	/**
	 * Records the number of successive acquires in the same
	 * thread. The thread is released only when the depth
	 * reaches zero.
	 */
	private int depth;
	/**
	 * Locks are sequentially ordered for debugging purposes.
	 */
	private static int nextLockNumber = 0;
	/**
	 * The thread of the operation that currently owns the lock.
	 */
	private Thread currentOperationThread;
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
	protected OrderedLock(LockManager manager) {
		this.manager = manager;
		this.number = nextLockNumber++;
	}
	/**
	 * Acquires this lock.  Callers will block indefinitely
	 * until the lock becomes available or this thread is interrupted. 
	 */
	public void acquire() throws InterruptedException {
		Semaphore semaphore = createSemaphore();
		if (semaphore != null) {
			if (DEBUG)
				System.out.println("[" + Thread.currentThread() + "] Operation waiting to be executed... :-/"); //$NON-NLS-1$ //$NON-NLS-2$
			//free all greater locks that this thread currently holds
			LockManager.LockState[] oldLocks = manager.suspendGreaterLocks(this);
			//now it is safe to acquire this lock
			doAcquire(semaphore);
			//finally, re-acquire the greater locks that we freed earlier
			if (oldLocks != null) {
				for (int i = 0; i < oldLocks.length; i++) {
					oldLocks[i].resume();
				}
			}
			if (DEBUG)
				System.out.println("[" + Thread.currentThread() + "] Operation started... :-)"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		depth++;
	}
	/**
	 * Returns null if acquired and a Semaphore object otherwise.
	 */
	protected synchronized Semaphore createSemaphore() {
		//return null if we already own the lock
		if (currentOperationThread == Thread.currentThread())
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
	protected void doAcquire(Semaphore semaphore) throws InterruptedException {
		if (semaphore != null) {
			try {
				semaphore.acquire();
			} catch (InterruptedException e) {
				if (DEBUG)
					System.out.println("[" + Thread.currentThread() + "] Operation interrupted while waiting... :-|"); //$NON-NLS-1$ //$NON-NLS-2$
				throw e;
			}
			updateCurrentOperation();
		}
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
	 * Force this lock to release, regardless of depth.  Returns the current depth.
	 */
	protected synchronized int doRelease() {
		int oldDepth = depth;
		depth = 0;
		Semaphore next = (Semaphore) operations.peek();
		currentOperationThread = null;
		if (next != null)
			next.release();
		return oldDepth;
	}
	/**
	 * Returns the thread of the current operation, or null if
	 * there is no current operation
	 */
	protected synchronized Thread getCurrentOperationThread() {
		return currentOperationThread;
	}

	/**
	 * Releases this lock allowing others to acquire it.
	 * @see #acquire
	 */
	public synchronized void release() {
		Assert.isTrue(currentOperationThread == Thread.currentThread(), "OrderedLock released by wrong thread"); //$NON-NLS-1$
		//only release the lock when the depth reaches zero
		if (--depth == 0) {
			doRelease();
		}
	}
	/**
	 * Forces the lock to be at the given depth.  Used when re-acquiring a suspended
	 * lock.
	 */
	protected void setDepth(int newDepth) {
		this.depth = newDepth;
	}
	/**
	 * For debugging purposes only.
	 */
	public String toString() {
		return "OrderedLock(" + number + ")"; //$NON-NLS-1$ //$NON-NLS-2$
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