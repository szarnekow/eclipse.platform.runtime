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

import org.eclipse.core.runtime.IStatus;

/**
 * Callback interface for clients interested in being notified of
 * the progress of jobs being managed by the job manager.
 * 
 * @see IJobManager#addListener(IJobListener)
 * @see IJobManager#removeListener(IJobListener)
 */
public interface IJobListener {
	/**
	 * Notification that a job is about to be run.
	 * Listeners are allowed to sleep, cancel, or change the priority of the 
	 * job before it is started (and as a result may prevent
	 * the run from actually occurring).
	 * 
	 * @param job the job that is about to be run.
	 */
	public void aboutToRun(Job job);
	/**
	 * Notification that a job was previously sleeping and has now been rescheduled
	 * to run.
	 * 
	 * @param job the job that has been awakened
	 */
	public void awake(Job job);
	/**
	 * Notification that a job has completed execution, either due to cancelation, successful
	 * completion, or failure.  The supplied status object indicates the reason for failure.
	 * 
	 * @param job the job that has stopped.
	 * @param result the result from the job's <code>run</code>
	 * method.
	 */
	public void done(Job job, IStatus result);
	/**
	 * Notification that a job has started running.
	 * 
	 * @param job the job that has started.
	 */
	public void running(Job job);
	/**
	 * Notification that a job has been added to the queue of 
	 * scheduled jobs.  Listeners are allowed to sleep, cancel, or
	 * change the priority of the given job before it has a chance to run.
	 * 
	 * @param job the job that is about to be added.
	 */
	public void scheduled(Job job);
	/**
	 * Notification that a job was waiting to run and has now been put in the
	 * sleeping state.
	 * 
	 * @param job the job that has been put to sleep
	 */
	public void sleeping(Job job);
}