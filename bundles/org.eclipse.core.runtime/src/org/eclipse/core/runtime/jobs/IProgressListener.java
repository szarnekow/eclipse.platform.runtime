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


/**
 * A progress listener is notified of progress on a running job.
 */
public interface IProgressListener {
	/**
	 * Notification that a job has invoked beginTask on its progress monitor.
	 * 
	 * @see IProgressMonitor#beginTask
	 */
	public void beginTask(Job job, String name, int totalWork);
	/**
	 * Notification that a job has invoked done on its progress monitor.
	 * 
	 * @see IProgressMonitor#done
	 */
	public void done(Job job);
	/**
	 * Notification that a job has invoked setTaskName on its progress monitor.
	 * 
	 * @see IProgressMonitor#setTaskName
	 */
	public void setTaskName(Job job, String name);
	/**
	 * Notification that a job has invoked subTask on its progress monitor.
	 * 
	 * @see IProgressMonitor#subTask
	 */
	public void subTask(Job job, String name);
	/**
	 * Notification that a job has invoked worked on its progress monitor.
	 * 
	 * @see IProgressMonitor#worked
	 */
	public void worked(Job job, double work);
}
