/**********************************************************************
 * Copyright (c) 2003 IBM Corporation and others. All rights reserved.   This
 * program and the accompanying materials are made available under the terms of
 * the Common Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.core.runtime.jobs;

/**
 * A job family is a group of related jobs.  A family of jobs can be manipulated in
 * many of the same ways as jobs. That is, a family of jobs can be paused, resumed, 
 * or canceled together.  A job is associated with a family when a family is provided
 * to the job manager's schedule method.  The job will be associated with that family
 * until it has finished running, or is canceled.  If the job is run again, it can be run with 
 * a different family, or with no family.
 * 
 * Jobs within a family are guaranteed to be started in the same order they were
 * scheduled.
 * 
 * All jobs scheduled with a family will adopt the priority setting of the family.  The
 * priority of the individual job can still be viewed and changed, but it is ignored for
 * scheduling purposes.
 * 
 * A job family can optionally be exclusive.  An exclusive family only allows one job
 * from that family to be running at any given time.  Thus a family can be used 
 * to make a set of jobs run sequentially.
 * 
 */
public interface IJobFamily {
	/**
	 * Stops all jobs in this family.  Jobs in the family that are currently waiting,
	 * will be removed from the queue.  Paused jobs will be discarded without having 
	 * a chance to resume.  Currently executing jobs will be asked to
	 * stop but there is no guarantee that they will do so.
	 */
	public void cancel();
	/**
	 * Returns the priority of this family.
	 * 
	 * @return the priority of the family.  One of INTERACTIVE, SHORT, LONG, BUILD, 
	 * 	or DECORATE.
	 */
	public int getPriority();
	/**
	 * Returns whether this job family is exclusive.  An exclusive job
	 * family will only allow one job from the family to be running at any given time.
	 * 
	 * @return true if the family is exclusive, and false otherwise
	 */
	public boolean isExclusive();
	/**
	 * Returns all waiting, executing and paused jobs belonging
	 * to the given family. 
	 * 
	 * If no jobs are found, an empty array is returned.
	 */
	public Job[] members();
	
	/**
	 * Requests that all jobs in this family be paused.
	 * Jobs currently waiting to be run will be removed
	 * from the queue.  This method has no effect for jobs that are
	 * already running, paused, or finished execution.
	 * 
	 * Paused jobs can be resumed.
	 */
	public void pause();
	/**
	 * Resumes execution of all jobs in this family.  Only jobs
	 * that are currently paused can be resumed.
	 * If there are no paused jobs in the family, this request is ignored.
	 * @param job
	 */
	public void resume();
	/**
	 * Sets the priority of this family.  This will not affect the execution of
	 * jobs that are already running, but it will affect the scheduleding jobs that are
	 * waiting to be run.
	 * 
	 * @param priority the new job priority.  One of
	 * INTERACTIVE, SHORT, LONG, BUILD, or DECORATE.
	 */
	public void setPriority(int i);
}
