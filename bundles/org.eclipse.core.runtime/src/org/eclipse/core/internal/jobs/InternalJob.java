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
public abstract class InternalJob {
	private IJobFamily family;
	private static final JobManager manager = JobManager.getInstance();
	private static int nextJobNumber = 0;
	
	private boolean paused = false;
	private int priority = Job.NONE;
	private int status = Job.NONE;
	private final int jobNumber = nextJobNumber++;

	public boolean cancel() {
		return manager.cancel((Job)this);
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
	public boolean pause() {
		return manager.pause(this);
	}
	public void resume() {
		paused = false;
	}
	public void setFamily(IJobFamily family) {
		this.family = family;
	}
	protected void setPriority(int i) {
		priority = i;
	}
	/*package*/ void setState(int i) {
		status = i;
	}
	public String toString() {
		return "Job(" + jobNumber + ")";
	}
}