package org.eclipse.core.internal.locks;

import java.util.ArrayList;

import org.eclipse.core.internal.runtime.Assert;

/**
 * Manages a set of locks and ensures that deadlock never occurs.
 */
public class LockManager {
	private final ArrayList locks = new ArrayList();
	public LockManager() {
	}
	/**
	 * The current thread is attempting to acquire the given lock.
	 * If this thread holds any locks greater than the given lock,
	 * release them, and build a list of locks that need to be
	 * acquired, in ascending order.  This ensures deadlock
	 * can never occur because locks are always required in
	 * ascending order.
	 */
	public synchronized Lock[] computeLocksToAcquire(Lock toLock) {
		Thread currentThread = Thread.currentThread();
		//find the given lock in the lock list
		int lockCount = locks.size();
		int i = locks.indexOf(toLock);
		if (i == -1) {
			//we didn't find the given lock
			Assert.isTrue(false, "Lock not found: " + toLock); //$NON-NLS-1$
			return null;
		}
		ArrayList toAcquire = new ArrayList();
		//we always want to acquire the requested lock
		toAcquire.add(locks.get(i++));
		//gather all locks greater than the requested lock
		for (; i < lockCount; i++) {
			Lock lock = (Lock) locks.get(i);
			if (lock.getCurrentOperationThread() == currentThread) {
				toAcquire.add(lock);
				//release this lock so contending threads can access it
				lock.release();
			}
		}
		return (Lock[]) toAcquire.toArray(new Lock[toAcquire.size()]);
	}
	public synchronized Lock newLock() {
		Lock result = new Lock(this);
		locks.add(result);
		return result;
	}
}
