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
import org.eclipse.core.runtime.jobs.IJobListener;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Responsible for notifying all job listeners about job lifecycle events.
 */
public class JobListeners implements IJobListener {
	/**
	 * The global job listeners.
	 */
	private final List global = Collections.synchronizedList(new ArrayList());
	
	public void aboutToRun(Job job) {
		for (Iterator it = global.iterator(); it.hasNext();)
			((IJobListener) it.next()).aboutToRun(job);
		List local = job.getListeners();
		if (local != null) {
			for (Iterator it = local.iterator(); it.hasNext();)
				((IJobListener) it.next()).aboutToRun(job);
		}
	}
	public void add(IJobListener listener) {
		global.add(listener);
	}
	public void awake(Job job) {
		for (Iterator it = global.iterator(); it.hasNext();)
			((IJobListener) it.next()).awake(job);
		List local = job.getListeners();
		if (local != null) {
			for (Iterator it = local.iterator(); it.hasNext();)
				((IJobListener) it.next()).awake(job);
		}
	}
	public void done(Job job, IStatus result) {
		for (Iterator it = global.iterator(); it.hasNext();)
			((IJobListener) it.next()).done(job, result);
		List local = job.getListeners();
		if (local != null) {
			for (Iterator it = local.iterator(); it.hasNext();)
				((IJobListener) it.next()).done(job, result);
		}
	}
	public void remove(IJobListener listener) {
		global.remove(listener);
	}
	public void running(Job job) {
		for (Iterator it = global.iterator(); it.hasNext();)
			((IJobListener) it.next()).running(job);
		List local = job.getListeners();
		if (local != null) {
			for (Iterator it = local.iterator(); it.hasNext();)
				((IJobListener) it.next()).running(job);
		}
	}
	public void scheduled(Job job) {
		for (Iterator it = global.iterator(); it.hasNext();)
			((IJobListener) it.next()).scheduled(job);
		List local = job.getListeners();
		if (local != null) {
			for (Iterator it = local.iterator(); it.hasNext();)
				((IJobListener) it.next()).scheduled(job);
		}
	}
	public void sleeping(Job job) {
		for (Iterator it = global.iterator(); it.hasNext();)
			((IJobListener) it.next()).sleeping(job);
		List local = job.getListeners();
		if (local != null) {
			for (Iterator it = local.iterator(); it.hasNext();)
				((IJobListener) it.next()).sleeping(job);
		}
	}
}