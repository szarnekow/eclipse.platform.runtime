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

import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Maintains a pool of worker threads.  Threads are constructed lazily as required,
 * and are eventually discarded if not in use for awhile.  This class maintains the
 * thread creation/destruction policies for the job manager.
 * 
 * 
 */
class WorkerPool {
//	private static final int MIN_THREADS = 1;
	private static final int MAX_THREADS = 4;
	private boolean running = false;
	private ArrayList threads = new ArrayList();
	private int sleepingThreads = 0;
	/**
	 * Threads not used by their best before timestamp are destroyed.
	 */
	private static final int BEST_BEFORE = 60000;

	private JobManager manager;

	protected WorkerPool(JobManager manager) {
		this.manager = manager;
		running = true;
	}
	protected void endJob(Job job, IStatus result) {
		manager.endJob(job, result);
	}
	protected synchronized void endWorker(Worker worker) {
		threads.remove(worker);
	}
	protected IProgressMonitor getProgressHandler() {
		return manager.getProgressHandler();
	}
	/**
	 * Notfication that a job has been added to the queue.  Wake a worker,
	 * creating a new worker if necessary
	 */
	protected synchronized void jobQueued(InternalJob job) {
		//if there is a thread that's not busy, wake it up
		if (sleepingThreads > 0) {
			notify();
			return;
		}
		//create a thread if we're under the max size or job is high priority
		if (threads.size() < MAX_THREADS || job.getPriority() == Job.INTERACTIVE) {
			Worker worker = new Worker(this);
			threads.add(worker);
			worker.start();
			return;
		}
	}
	protected void shutdown() {
		running = false;
	}
	/**
	 * Sleep until expiry or until woken
	 */
	private synchronized void sleep() {
		sleepingThreads++;
		try {
			wait(BEST_BEFORE);
		} catch (InterruptedException e) {
		} finally {
			sleepingThreads--;
		}
	}
	/**
	 * Returns a new job to run.  Returns null if the thread should die.
	 */
	protected Job startJob() {
		//if we're above capacity, kill the thread
		synchronized (this) {
			if (!running || threads.size() > MAX_THREADS) {
				return null;
			}
		}
		Job job = manager.startJob();
		if (job == null) {
			sleep();
			job = manager.startJob();
		}
		return job;
	}
}
