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

import org.eclipse.core.runtime.jobs.Job;

public class Worker extends Thread {
	private final JobManager jobManager;
	private volatile Job currentJob;
	/**
	 *
	 */
	public Worker(JobManager manager) {
		super();
		this.jobManager = manager;
	}
	/**
	 * Returns the currently running job, or null if none.
	 */
	public Job currentJob() {
		return currentJob;
	}
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		while (true) {
			currentJob = jobManager.startJob();
			//if job is null we've been shutdown
			if (currentJob == null)
				return;
			int result = currentJob.run(jobManager.getProgressHandler());
			jobManager.endJob(currentJob, result);
			currentJob = null;
		}
	}
}
