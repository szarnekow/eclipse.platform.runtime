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

import java.util.*;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Internal implementation class for jobs.
 */
public abstract class InternalJob implements Comparable {
	private static final JobManager manager = JobManager.getInstance();
	private static int nextJobNumber = 0;
	private IStatus parentResult;
	private List children;

	private final int jobNumber = nextJobNumber++;

	private int priority = Job.LONG;
	/**
	 * If the job is waiting, this represents the time the job should start by.  If
	 * this job is sleeping, this represents the time the job should wake up.
	 */
	private long startTime;
	private int status = Job.NONE;
	protected void addChild(Job job) {
		if (children == null)
			children = Collections.synchronizedList(new ArrayList(2));
		children.add(job);
	}

	public boolean cancel() {
		return manager.cancel((Job) this);
	}
	public final int compareTo(Object otherJob) {
		return (int) (startTime - ((InternalJob) otherJob).startTime);
	}
	/**
	 * Returns the children of this job, or null if this job has no children
	 */
	final Job[] getChildren() {
		if (children == null)
			return null;
		return (Job[]) children.toArray(new Job[children.size()]);
	}
	protected IStatus getParentResult() {
		return parentResult;
	}
	protected int getPriority() {
		return priority;
	}
	/*package*/
	final long getStartTime() {
		return startTime;
	}
	protected int getState() {
		return status;
	}
	void internalSetPriority(int newPriority) {
		this.priority = newPriority;
	}
	protected void removeChild(Job job) {
		if (children == null)
			return;
		children.remove(job);
		if (children.size() == 0)
			children = null;
	}
	public void schedule(long delay) {
		manager.schedule(this, delay);
	}
	protected void setPriority(int NewPriority) {
		manager.setPriority(this, NewPriority);
	}
	final void setParentResult(IStatus result) {
		this.parentResult = result;
	}
	final void setStartTime(long time) {
		startTime = time;
	}
	final void setState(int i) {
		status = i;
	}
	protected boolean sleep() {
		return manager.sleep(this);
	}
	public String toString() {
		return "Job(" + jobNumber + ")"; //$NON-NLS-1$//$NON-NLS-2$
	}
	protected void wakeUp() {
		manager.wakeUp(this);
	}
}