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
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.core.runtime.jobs.IJobListener;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Internal implementation class for jobs.
 */
public abstract class InternalJob extends ListEntry implements Comparable {
	private static final JobManager manager = JobManager.getInstance();
	private static int nextJobNumber = 0;

	private boolean asyncFinish = false;
	private final int jobNumber = nextJobNumber++;
	private List listeners;
	private int priority = Job.LONG;
	private ISchedulingRule schedulingRule;
	/**
	 * If the job is waiting, this represents the time the job should start by.  If
	 * this job is sleeping, this represents the time the job should wake up.
	 */
	private long startTime;
	private int state = Job.NONE;
	/* (non-Javadoc)
	 * @see Job#addJobListener(IJobListener)
	 */
	public void addJobListener(IJobListener listener) {
		if (listeners == null)
			listeners = Collections.synchronizedList(new ArrayList(2));
		listeners.add(listener);
	}
	public boolean cancel() {
		return manager.cancel((Job) this);
	}
	public final int compareTo(Object otherJob) {
		return (int) (startTime - ((InternalJob) otherJob).startTime);
	}
	protected void done(IStatus result) {
		//ignore if not registered for asynchronous finish
		if (!asyncFinish)
			return;
		asyncFinish = false;
		manager.endJob((Job)this, result);
	}
	/**
	 * Returns the job listeners that are only listening to this job.  Returns null
	 * if this job has no listeners.
	 */
	public List getListeners() {
		return listeners;
	}
	protected int getPriority() {
		return priority;
	}
	public ISchedulingRule getRule() {
		return schedulingRule;
	}
	/*package*/
	final long getStartTime() {
		return startTime;
	}
	protected int getState() {
		return state;
	}
	void internalSetPriority(int newPriority) {
		this.priority = newPriority;
	}
	/* (non-Javadoc)
	 * @see Job#removeJobListener(IJobListener)
	 */
	public void removeJobListener(IJobListener listener) {
		if (listeners != null)
			listeners.remove(listener);
		if (listeners.isEmpty())
			listeners = null;
	}
	public void schedule(long delay) {
		manager.schedule(this, delay);
	}
	public void setAsyncFinish() {
		asyncFinish = true;
	}
	protected void setPriority(int NewPriority) {
		manager.setPriority(this, NewPriority);
	}
	protected void setRule(ISchedulingRule rule) {
		schedulingRule = rule;
	}
	final void setStartTime(long time) {
		startTime = time;
	}
	final void setState(int i) {
		state = i;
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