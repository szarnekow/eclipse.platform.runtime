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

import org.eclipse.core.runtime.jobs.IJobFamily;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Internal implementation class for jobs.
 */
public abstract class InternalJob implements Comparable {
	private IJobFamily family;
	private static final JobManager manager = JobManager.getInstance();
	private static int nextJobNumber = 0;
	/**
	 * If the job is waiting, this represents the time the job should start by.  If
	 * this job is sleeping, this represents the time the job should wake up.
	 */
	private long startTime;
	
	private int priority = Job.LONG;
	private int status = Job.NONE;
	private final int jobNumber = nextJobNumber++;

	public boolean cancel() {
		return manager.cancel((Job)this);
	}
	public final int compareTo(Object otherJob) {
		return (int)(startTime - ((InternalJob)otherJob).startTime);
	}
	public IJobFamily getFamily() {
		return family;
	}
	protected int getPriority() {
		return priority;
	}
	protected int getState() {
		return status;
	}
	void internalSetPriority(int newPriority) {
		this.priority = newPriority;
	}
	public boolean sleep() {
		return manager.sleep(this);
	}
	public void wakeUp() {
		manager.wakeUp(this);
	}
	public void setFamily(IJobFamily family) {
		this.family = family;
	}
	protected void setPriority(int NewPriority) {
		manager.setPriority(this, NewPriority);
	}
	/*package*/ final void setState(int i) {
		status = i;
	}
	public String toString() {
		return "Job(" + jobNumber + ")";  //$NON-NLS-1$//$NON-NLS-2$
	}
	/*package*/ final long getStartTime() {
		return startTime;
	}

	/*package*/ final void setStartTime(long time) {
		startTime = time;
	}

}