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

public class JobManager implements IJobManager {
	private static JobManager instance;
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
	/**
	 * Paused jobs that have arrived at the front of the queue.
	 */
	private final HashSet paused = new HashSet();

	/**
	 * The pool of worker threads.
	 */
	private WorkerPool pool;

	private final ProgressHandler progressHandler = new ProgressHandler(this);
	
	private final LockManager lockManager = new LockManager();

	private boolean running = false;

	/**
	 * jobs that are waiting to be run
	 */
	private final Queue waiting = new Queue();

	public static JobManager getInstance() {
		if (instance == null) {
			//ensure we don't start two job managers
			synchronized (JobManager.class) {
				if (instance == null) {
					instance = new JobManager();
					instance.startup();
				}
			}
		}
		return instance;
	}
	/**
	 * Shuts down the running job manager, if any.
	 */
	public static void shutdownIfRunning() {
		if (instance != null)
			instance.shutdown();
	}
	private JobManager() {
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
			wasWaiting = waiting.contains(job);
			oldState = job.getState();
			((InternalJob) job).setState(Job.NONE);
		}
		//only notify listeners if the job was waiting
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
	/**
	 * Removes and returns the first waiting job in the queue. If the queue is empty,
	 * this method blocks until a job becomes available.  Returns null only if
	 * the job manager has been shutdown.
	 */
	private Job nextJob() {
		synchronized (lock) {
			if (!running)
				return null;
			//spin until we find a valid job or queue is empty
			while (true) {
				Job job = (Job) waiting.dequeue();
				if (job == null)
					return null;
				if (prepareToRun(job))
					return job;
			}
		}
	}
	/* (non-Javadoc)
	 * @see IJobManager#newJobFamily(java.lang.String)
	 */
	public IJobFamily newJobFamily(int priority, boolean exclusive) {
		return new JobFamily(priority, exclusive);
	}
	/* (non-Javadoc)
	 * @see IJobManager#newLock(java.lang.String)
	 */
	public ILock newLock() {
		return lockManager.newLock();
	}
	/**
	 * Request to pause the given job. Return true if the job was successfully paused.
	 * @param job
	 */
	boolean pause(InternalJob job) {
		synchronized (lock) {
			//cannot be paused if it is already running
			if (job.getState() == Job.RUNNING)
				return false;
			job.setState(Job.PAUSED);
		}
		IJobListener[] listeners = getJobListeners();
		for (int i = 0; i < listeners.length; i++) {
			listeners[i].paused((Job)job);
		}
		return true;
	}
	/* (non-Javadoc)
	 * @see IJobManager#pause(java.lang.String)
	 */
	public void pause(String family) {
	}
	/**
	 * If the given job is ready to be run, make sure the state is RUNNING and
	 * return true.  If the job cannot be run, handle it appropriately and return false.
	 */
	private boolean prepareToRun(Job job) {
		if (job == null)
			return false;
		synchronized (lock) {
			switch (job.getState()) {
				case Job.WAITING :
					 ((InternalJob) job).setState(Job.RUNNING);
					return true;
				case Job.RUNNING :
					return true;
				case Job.PAUSED :
					paused.add(job);
				case Job.CANCELED :
				default :
					return false;
			}
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
	 * @see org.eclipse.core.runtime.jobs.IJobManager#resume(java.lang.String)
	 */
	public void resume(String family) {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobManager#schedule(ob)
	 */
	public void schedule(Job job) {
		schedule(job, 0);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobManager#schedule(Job, JobFamily)
	 */
	public void schedule(Job job, IJobFamily family) {
		((InternalJob) job).setFamily(family);
		schedule(job);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobManager#schedule(Job, long)
	 */
	public void schedule(Job job, long delay) {
		Assert.isNotNull(job, "Job is null"); //$NON-NLS-1$
		synchronized (lock) {
			allJobs.add(job);
		}
		//notify listeners outside sync block
		IJobListener[] listeners = getJobListeners();
		for (int i = 0; i < listeners.length; i++) {
			listeners[i].aboutToSchedule(job);
		}
		synchronized (lock) {
			//if the job is removed from allJobs, then it has been canceled
			if (!allJobs.contains(job))
				return;
			//if job is already paused, add it to the list of paused jobs
			if (job.getState() == Job.PAUSED) {
				paused.add(job);
				return;
			}
			((InternalJob)job).setState(Job.WAITING);
			waiting.enqueue(job);
			pool.jobQueued(job);
		}
	}
	/**
	 * Shuts down the job manager.  Currently running jobs will be told
	 * to stop, but worker threads may still continue processing.
	 */
	private void shutdown() {
		synchronized (lock) {
			running = false;
			//clean up
			pool.shutdown();
			paused.clear();
			waiting.clear();
			//discard all jobs (progress callbacks from running jobs
			//will now think the jobs are canceled, and should terminate
			//in a timely fashion)
			allJobs.clear();
			//wake up all workers so they know it's time to go home
			lock.notifyAll();
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
			if (!prepareToRun(job))
				continue;
			//get the listeners again because they may have been changed
			listeners = getJobListeners();
			for (int i = 0; i < listeners.length; i++) {
				listeners[i].running(job);
			}
			if (!prepareToRun(job))
				continue;
			return job;
		}
	}

	/**
	 * Starts the job manager, with the given number of worker threads.
	 */
	private void startup() {
		synchronized (lock) {
			running = true;
			pool = new WorkerPool(this);
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
}