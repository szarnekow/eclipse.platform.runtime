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
	 * Listeners are allowed to pause, stop, or change the priority of the 
	 * given job before it is started (and as a result may prevent
	 * the run from actually occurring).
	 * @param job the job that is about to be run.
	 */
	public void aboutToRun(Job job);
	/**
	 * Notification that the given job is being added to the queue of 
	 * scheduled jobs.  Listeners are allowed to pause, stop, or
	 * change the priority of the given job before it has a chance to run.
	 * @param job the job that is about to be added.
	 */
	public void aboutToSchedule(Job job);
	/**
	 * Notification that a job has stopped running.
	 * @param job the job that has stopped.
	 * @param result the result from the job's <code>run</code>
	 * method.
	 */
	public void finished(Job job, IStatus result);
	/**
	 * Notification that a job has started running.
	 * @param job the job that has started.
	 */
	public void running(Job job);
	/**
	 * Notification that a job was waiting to run and has now been paused.
	 * @param job the job that has been paused
	 */
	public void paused(Job job);
	/**
	 * Notification that a job was previously paused and has now been rescheduled
	 * to run.
	 * @param job the job that has been resumed
	 */
	public void resumed(Job job);
}