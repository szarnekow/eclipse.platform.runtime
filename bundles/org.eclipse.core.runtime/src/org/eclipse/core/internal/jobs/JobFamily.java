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
 * Standard implementation of the IJobFamily interface.
 */
class JobFamily implements IJobFamily {
	private int priority;
	private boolean exclusive;
	public JobFamily(int priority, boolean exclusive) {
		this.priority = priority;
		this.exclusive = exclusive;
	}
	public void cancel() {
	}
	public int getPriority() {
		return priority;
	}
	public boolean isExclusive() {
		return exclusive;
	}
	public Job[] members() {
		return null;
	}
	public void pause() {
	}
	public void resume() {
	}
	public void setPriority(int newPriority) {
		this.priority = newPriority;
	}
}