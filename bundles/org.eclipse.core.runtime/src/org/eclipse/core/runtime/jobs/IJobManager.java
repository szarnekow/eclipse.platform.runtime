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
package org.eclipse.core.runtime.jobs;

import org.eclipse.core.runtime.IProgressMonitor;

public interface IJobManager {
	/**
	 * Registers a job listener with the job manager.  
	 * Has no effect if an identical listener is already registered.
	 * 
	 * @param listener the listener to be added.
	 */
	public void addJobListener(IJobListener listener);
	/**
	 * Registers a progress listener with the job manager.  
	 * Has no effect if an identical listener is already registered.
	 * 
	 * @param listener the listener to be added.
	 */
	public void addProgressListener(IProgressListener listener);
	/**
	 * Returns the job that is currently running in this thread, or null if there
	 * is no currently running job.
	 * @param listener
	 */
	public Job currentJob();

	/**
	 * Removes a job listener from the job manager.  
	 * Has no effect if an identical listener is not already registered.
	 * 
	 * @param listener the listener to be removed.
	 */
	public void removeJobListener(IJobListener listener);
	/**
	 * Removes a progress listener from the job manager.  
	 * Has no effect if an identical listener is not already registered.
	 * 
	 * @param listener the listener to be removed.
	 */
	public void removeProgressListener(IProgressListener listener);

	/**
	 * Adds the given job to the queue of waiting jobs. This method
	 * will return before the given job has had a chance to run.
	 * 
	 * @param job the job to add to the queue
	 */
	public void schedule(Job job);
	/**
	 * Adds the given job to the queue of waiting jobs after a delay has elapsed. 
	 * This method will return before the given job has had a chance to run.
	 * 
	 * @param job the job to add to the queue
	 * @param delay a delay in milliseconds before the job should be added to the
	 * queue of waiting jobs
	 */
	public void schedule(Job job, long delay);
	/**
	 * Adds the given job to the queue of waiting jobs. This method
	 * will return before the given job has had a chance to run. The job will be 
	 * associated with the given family until the job has finished running or has 
	 * been canceled.
	 * 
	 * @param job the job to add to the queue
	 * @param family the family the job should be associated with for this run
	 */
	public void schedule(Job job, IJobFamily family);
	/**
	 * Waits until the given job is finished.  This method will block
	 * the calling thread until the given job has finished executing.  
	 * Feedback on how the wait is progressing is provided to the given 
	 * progress monitor.
	 * @param job
	 */
	public void wait(Job job, IProgressMonitor monitor);
	/**
	 * Waits until all jobs of the given family are finished. 
	 * If a family of <code>null</code> is specified, waits until all waiting
	 * and executing jobs are finished.  This method will block the calling 
	 * thread until all such jobs have finished executing.  Feedback on how 
	 * the wait is progressing is provided to the given progress monitor.
	 * 
	 * Warning: this can result in starvation of the current thread if
	 * another thread continues to add jobs of the given family.
	 * @param job
	 */
	public void wait(String family, IProgressMonitor monitor);
}