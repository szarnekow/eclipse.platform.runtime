/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.jobs;

import java.util.*;

import org.eclipse.core.internal.runtime.Assert;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;

/**
 * Implementation of API type IJobManager
 * 
 * Implementation note: all the data structures of this class are protected
 * by a single lock object held as a private field in this class.  The JobManager
 * instance itself is not used because this class is publicly reachable, and third
 * party clients may try to sychronize on it.
 * 
 * The WorkerPool class uses its own monitor for synchronizing its data
 * structures. To avoid deadlock between the two classes, the JobManager
 * must NEVER call the worker pool while its own monitor is held.
 */
public class JobManager implements IJobManager {
	public static final boolean DEBUG = true;
	private static JobManager instance;
	protected static final long NEVER = Long.MAX_VALUE;
	/**
	 * Set of all jobs.
	 */
	private final Set allJobs = new HashSet();
	private final List listeners = Collections.synchronizedList(new ArrayList());
	/**
	 * The lock for synchronizing all activity in the job manager.  To avoid deadlock,
	 * this lock must never be held for extended periods, and must never be
	 * held while third party code is being called.
	 */
	private final Object lock = new Object();

	private final LockManager lockManager = new LockManager();

	/**
	 * The pool of worker threads.
	 */
	private WorkerPool pool;

	private final ProgressHandler progressHandler = new ProgressHandler(this);

	/**
	 * Flag to indicate that the system is still up and running.
	 */
	private boolean running = false;
	/**
	 * Jobs that are scheduled, but have not yet reached their scheduled start time.
	 */
	private final PriorityQueue sleeping = new PriorityQueue(10);
	/**
	 * jobs that are waiting to be run
	 */
	private final PriorityQueue waiting = new PriorityQueue(10);

	public static synchronized JobManager getInstance() {
		if (instance == null) {
			instance = new JobManager();
		}
		return instance;
	}
	public static synchronized void shutdown() {
		if (instance != null)
			instance.doShutdown();
		instance = null;
	}

	private JobManager() {
		synchronized (lock) {
			running = true;
			pool = new WorkerPool(this);
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobManager#addListener(org.eclipse.core.runtime.jobs.IJobListener)
	 */
	public void addJobListener(IJobListener listener) {
		listeners.add(listener);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobManager#addProgressListener(org.eclipse.core.runtime.jobs.IProgressListener)
	 */
	public void addProgressListener(IProgressListener listener) {
		progressHandler.addListener(listener);
	}
	/**
	 * Cancels a job
	 */
	boolean cancel(Job job) {
		int oldState;
		boolean wasWaiting;
		synchronized (lock) {
			if (!allJobs.remove(job))
				return true;
			wasWaiting = waiting.remove(job) || sleeping.remove(job);
			oldState = job.getState();
			((InternalJob) job).setState(Job.NONE);
		}
		//only notify listeners if the job was waiting or sleeping
		//(if the job is already running, then we notify when it finishes its run method)
		if (wasWaiting) {
			IJobListener[] listeners = getJobListeners();
			for (int i = 0; i < listeners.length; i++) {
				listeners[i].finished(job, Status.CANCEL_STATUS);
			}
		}
		//cancelation may only fail if the job is currently running
		return oldState == Job.RUNNING;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobManager#cancel(java.lang.String)
	 */
	public void cancel(String family) {
	}
	public Job currentJob() {
		Thread current = Thread.currentThread();
		if (current instanceof Worker)
			return ((Worker) current).currentJob();
		return null;
	}
	/**
	 * Returns the delay in milliseconds that a job with a given priority can
	 * tolerate waiting.
	 */
	private long delayFor(int priority) {
		//these values may need to be tweaked based on machine speed
		switch (priority) {
			case Job.INTERACTIVE :
				return 0L;
			case Job.SHORT :
				return 50L;
			case Job.LONG :
				return 100L;
			case Job.BUILD :
				return 500L;
			case Job.DECORATE :
				return 1000L;
			default :
				Assert.isTrue(false, "Job has invalid priority: " + priority); //$NON-NLS-1$
				return 0;
		}
	}
	/**
	 * Shuts down the job manager.  Currently running jobs will be told
	 * to stop, but worker threads may still continue processing.
	 */
	private void doShutdown() {
		synchronized (lock) {
			running = false;
			//clean up
			sleeping.clear();
			waiting.clear();
			//discard all jobs (progress callbacks from running jobs
			//will now think the jobs are canceled, and should terminate
			//in a timely fashion)
			allJobs.clear();
		}
		pool.shutdown();
	}
	/**
	 * Returns the next job to be run.  If no jobs are waiting to run,
	 * this method will block until a job is available.  The worker must
	 * call endJob when the job is finished running.
	 * @return
	 */
	void endJob(Job job, IStatus result) {
		synchronized (lock) {
			((InternalJob) job).setState(Job.NONE);
			allJobs.remove(job);
		}
		//schedule child jobs
		Job[] children = ((InternalJob) job).getChildren();
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				children[i].schedule();
			}
		}

		//notify listeners outside sync block
		IJobListener[] listeners = getJobListeners();
		for (int i = 0; i < listeners.length; i++) {
			listeners[i].finished(job, result);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobManager#find(java.lang.String)
	 */
	public Job[] find(String family) {
		return null;
	}
	private IJobListener[] getJobListeners() {
		synchronized (lock) {
			return (IJobListener[]) listeners.toArray(new IJobListener[listeners.size()]);
		}
	}
	IProgressMonitor getProgressHandler() {
		return progressHandler;
	}
	/* (non-Javadoc)
	 * @see IJobManager#newLock(java.lang.String)
	 */
	public ILock newLock() {
		return lockManager.newLock();
	}
	/**
	 * Removes and returns the first waiting job in the queue. Returns null if there
	 * are no items waiting in the queue
	 */
	private Job nextJob() {
		synchronized (lock) {
			//tickle the sleep queue to see if anyone wakes up
			long now = System.currentTimeMillis();
			InternalJob job = (InternalJob)sleeping.peek();
			while (job != null && job.getStartTime() < now) {
				sleeping.dequeue();
				job.setState(Job.WAITING);
				job.setStartTime(now + delayFor(job.getPriority()));
				waiting.enqueue(job);
				job = (InternalJob)sleeping.peek();
			}
			//return the first waiting job
			return (Job) waiting.dequeue();
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobManager#removeListener(org.eclipse.core.runtime.jobs.IJobListener)
	 */
	public void removeJobListener(IJobListener listener) {
		listeners.remove(listener);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobManager#removeProgressListener(org.eclipse.core.runtime.jobs.IProgressListener)
	 */
	public void removeProgressListener(IProgressListener listener) {
		progressHandler.removeListener(listener);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.Job#schedule(long)
	 */
	protected void schedule(InternalJob job, long delay) {
		Assert.isNotNull(job, "Job is null"); //$NON-NLS-1$
		synchronized (lock) {
			allJobs.add(job);
		}
		synchronized (lock) {
			//if the job is removed from allJobs, then it has been canceled
			if (!allJobs.contains(job))
				return;
			//if job is has been put to sleep, there is nothing to do
			if (job.getState() == Job.SLEEPING)
				return;
			if (delay > 0) {
				job.setState(Job.SLEEPING);
				job.setStartTime(System.currentTimeMillis() + delay);
				sleeping.enqueue(job);
			} else {
				job.setState(Job.WAITING);
				job.setStartTime(System.currentTimeMillis() + delayFor(job.getPriority()));
				waiting.enqueue(job);
			}
		}
		//call the pool outside sync block to avoid deadlock
		pool.jobQueued(job);

		//notify listeners outside sync block
		IJobListener[] listeners = getJobListeners();
		Job publicJob = (Job)job;
		for (int i = 0; i < listeners.length; i++) {
			listeners[i].scheduled(publicJob);
		}
	}
	/**
	 * Changes a job priority.
	 */
	void setPriority(InternalJob job, int newPriority) {
		synchronized (lock) {
			int oldPriority = job.getPriority();
			if (oldPriority == newPriority)
				return;
			job.internalSetPriority(newPriority);
			//if the job is waiting to run, reshuffle the queue
			if (job.getState() == Job.WAITING) {
				long oldStart = job.getStartTime();
				job.setStartTime(oldStart + (delayFor(newPriority) - delayFor(oldPriority)));
				waiting.resort(job);
			}
		}
	}
	/**
	 * Puts a job to sleep. Returns true if the job was successfully put to sleep.
	 */
	boolean sleep(InternalJob job) {
		synchronized (lock) {
			switch (job.getState()) {
				case Job.RUNNING :
					//cannot be paused if it is already running
					return false;
				case Job.SLEEPING :
					//update the job wake time
					job.setStartTime(NEVER);
					return true;
				case Job.WAITING :
					//put the job to sleep
					waiting.remove(job);
					job.setStartTime(NEVER);
					job.setState(Job.SLEEPING);
					sleeping.enqueue(job);
					//fall through and notify listeners
			}
		}
		IJobListener[] listeners = getJobListeners();
		for (int i = 0; i < listeners.length; i++) {
			listeners[i].sleeping((Job) job);
		}
		return true;
	}
	/* (non-Javadoc)
	 * @see IJobManager#sleep(String)
	 */
	public void sleep(String family) {
	}
	/**
	 * Returns the estimated time in milliseconds before the next job is scheduled
	 * to wake up. The result may be negative.  Returns JobManager.NEVER if
	 * there are no sleeping or waiting jobs.
	 */
	long sleepHint() {
		synchronized (lock) {
			if (waiting.peek() != null)
				return 0L;
			InternalJob next = (InternalJob)sleeping.peek();
			return next == null ? NEVER : next.getStartTime() - System.currentTimeMillis();
		}
	}

	/**
	 * Returns the next job to be run, or null if no jobs are waiting to run.
	 * The worker must call endJob when the job is finished running.  
	 */
	Job startJob() {
		while (true) {
			Job job = nextJob();
			if (job == null)
				return null;
			//must perform this outside sync block because it is third party code
			if (!job.shouldRun()) {
				job.cancel();
				continue;
			}
			//check for listener veto
			IJobListener[] listeners = getJobListeners();
			for (int i = 0; i < listeners.length; i++) {
				listeners[i].aboutToRun(job);
			}
			synchronized (lock) {
				//listeners may have canceled or put the job to sleep
				if (job.getState() != Job.WAITING)
					continue;
				((InternalJob) job).setState(Job.RUNNING);
			}
			//get the listeners again because they may have been changed
			listeners = getJobListeners();
			for (int i = 0; i < listeners.length; i++) {
				listeners[i].running(job);
			}
			return job;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobManager#wait(org.eclipse.core.runtime.jobs.Job, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void wait(Job job, IProgressMonitor monitor) {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobManager#wait(java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void wait(String family, IProgressMonitor monitor) {
	}
	/**
	 * Implementation of wakeUp()
	 */
	void wakeUp(InternalJob job) {
		synchronized (lock) {
			//cannot wake up if it is not sleeping
			if (job.getState() != Job.SLEEPING)
				return;
			sleeping.remove(job);
			job.setState(Job.WAITING);
			job.setStartTime(System.currentTimeMillis() + delayFor(job.getPriority()));
			waiting.enqueue(job);
		}
		IJobListener[] listeners = getJobListeners();
		for (int i = 0; i < listeners.length; i++) {
			listeners[i].awake((Job) job);
		}
	}
	/* (non-Javadoc)
	 * @see IJobFamily#wakeUp(String)
	 */
	public void wakeUp(String family) {
	}
}