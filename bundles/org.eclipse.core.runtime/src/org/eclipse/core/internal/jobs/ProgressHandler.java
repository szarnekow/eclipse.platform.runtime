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

import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.IProgressListener;

/**
 * The progress handler gathers progress from all running jobs and dispatches
 * updates to all registered progress listeners.
 */
class ProgressHandler implements IProgressMonitor {
	private final JobManager jobManager;
	private final ArrayList listeners = new ArrayList();
	/**
	 * Creates a new progress handler.
	 */
	ProgressHandler(JobManager manager) {
		this.jobManager = manager;
	}
	void addListener(IProgressListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IProgressMonitor#beginTask(java.lang.String, int)
	 */
	public void beginTask(String name, int totalWork) {
		Job job = jobForThread();
		IProgressListener[] listeners = listeners();
		for (int i = 0; i < listeners.length; i++) {
			listeners[i].beginTask(job, name, totalWork);
		}
	}
	/**
	 * Returns the job being run by the current thread, or null.
	 */
	private Job jobForThread() {
		Thread current = Thread.currentThread();
		if (!(current instanceof Worker))
			return null;
		return ((Worker)current).currentJob();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IProgressMonitor#done()
	 */
	public void done() {
		Job job = jobForThread();
		IProgressListener[] listeners = listeners();
		for (int i = 0; i < listeners.length; i++) {
			listeners[i].done(job);
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IProgressMonitor#internalWorked(double)
	 */
	public void internalWorked(double work) {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IProgressMonitor#isCanceled()
	 */
	public boolean isCanceled() {
		//canceled jobs are unknown to the job manager
		return jobForThread().getState() == Job.NONE;
	}
	/**
	 * Returns a copy of the list of listeners.
	 */
	private IProgressListener[] listeners() {
		IProgressListener[] result;
		synchronized (listeners) {
			result = (IProgressListener[])listeners.toArray(new IProgressListener[listeners.size()]);
		}
		return result;
	}
	void removeListener(IProgressListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IProgressMonitor#setCanceled(boolean)
	 */
	public void setCanceled(boolean value) {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IProgressMonitor#setTaskName(java.lang.String)
	 */
	public void setTaskName(String name) {
		Job job = jobForThread();
		IProgressListener[] listeners = listeners();
		for (int i = 0; i < listeners.length; i++) {
			listeners[i].setTaskName(job, name);
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IProgressMonitor#subTask(java.lang.String)
	 */
	public void subTask(String name) {
		Job job = jobForThread();
		IProgressListener[] listeners = listeners();
		for (int i = 0; i < listeners.length; i++) {
			listeners[i].subTask(job, name);
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IProgressMonitor#worked(int)
	 */
	public void worked(int work) {
		Job job = jobForThread();
		IProgressListener[] listeners = listeners();
		for (int i = 0; i < listeners.length; i++) {
			listeners[i].worked(job, work);
		}
	}
}
