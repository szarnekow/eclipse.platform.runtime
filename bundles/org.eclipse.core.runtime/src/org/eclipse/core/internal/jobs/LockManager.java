package org.eclipse.core.internal.jobs;

import java.util.ArrayList;

import org.eclipse.core.internal.runtime.Assert;

/**
 * Manages a set of locks and ensures that deadlock never occurs.
 */
public class LockManager {
	/**
	 * This class captures the state of suspended locks.  Locks are suspended if
	 * a thread tries to acquire locks out of order.
	 */
	public static class LockState {
		private OrderedLock lock;
		private int depth;
		/**
		 * Suspends ownership of the given lock, and returns the saved state.
		 */
		protected static LockState suspend(OrderedLock lock) {
			LockState state = new LockState();
			state.lock = lock;
			state.depth = lock.doRelease();
			return state;
		}
		/**
		 * Re-acquires a suspended lock and reverts to the given state.
		 */
		protected void resume() throws InterruptedException {
			lock.doAcquire(lock.createSemaphore());
			lock.setDepth(depth);
		}
	}
	private final ArrayList locks = new ArrayList();
	public LockManager() {
	}
	/**
	 * The current thread is attempting to acquire the given lock.
	 * If this thread holds any locks greater than the given lock,
	 * release them, and build a list of locks that need to be
	 * acquired, in ascending order.  This ensures deadlock
	 * can never occur because locks are always acquired in
	 * ascending order.
	 * @return the list of locks that need to be acquired, or null if
	 * no other locks need to be acquired
	 */
	public synchronized LockState[] suspendGreaterLocks(OrderedLock toLock) {
		Thread currentThread = Thread.currentThread();
		//find the given lock in the lock list
		int lockCount = locks.size();
		int i = locks.indexOf(toLock);
		if (i == -1) {
			//we didn't find the given lock
			Assert.isTrue(false, "OrderedLock not found: " + toLock); //$NON-NLS-1$
			return null;
		}
		//gather all locks greater than the requested lock
		i++;
		ArrayList toAcquire = null;
		for (; i < lockCount; i++) {
			OrderedLock lock = (OrderedLock) locks.get(i);
			if (lock.getCurrentOperationThread() == currentThread) {
				if (toAcquire == null)
					toAcquire = new ArrayList();
				//release this lock so contending threads can access it, and remember old depth
				toAcquire.add(LockState.suspend(lock));
			}
		}
		if (toAcquire == null)
			return null;
		return (LockState[]) toAcquire.toArray(new LockState[toAcquire.size()]);
	}
	public synchronized OrderedLock newLock() {
		OrderedLock result = new OrderedLock(this);
		locks.add(result);
		return result;
	}
}