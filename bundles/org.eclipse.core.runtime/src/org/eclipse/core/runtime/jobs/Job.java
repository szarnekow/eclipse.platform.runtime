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

import org.eclipse.core.internal.jobs.InternalJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

/**
 * Jobs are units of runnable work that can be scheduled to be run with the job
 * manager.  The same job instance can be scheduled to run several times, although
 * rescheduling a job instance that is already waiting will move it to the back of the queue.
 * 
 * Jobs have a state that indicates what they are currently doing.  When constructed,
 * jobs start with a state value of <code>NONE</code>.  When a job is scheduled
 * to be run, it moves into the <code>WAITING</code> state.  When a job starts
 * running, it moves into the <code>RUNNING</code>.  When execution finishes
 * (either normally or through cancelation), the state changes back to 
 * <code>NONE</code>.  
 * 
 * If the job is successfully paused at any time, it moves into the 
 * <code>PAUSED</code> state.  The job will remain in the paused state until it is 
 * either resumed or canceled.  A running job cannot be paused.
 * 
 * Jobs can be assigned a priority that is used as a hint about how the job should
 * be scheduled.  There is no guarantee that jobs of one priority will be run before
 * all jobs of lower priority.  The javadoc for the various priority constants provide
 * more detail about what each priority means.  By default, jobs start in the 
 * <code>LONG</code> priority class.
 * 
 */
public abstract class Job extends InternalJob {
	/* Job priorities */
	/** 
	 * Job priority constant (value 10) for interactive jobs.
	 * Interactive jobs generally have priority over all other jobs.
	 * Interactive jobs must be relatively fast running in order to avoid
	 * blocking other interactive jobs from running.
	 * 
	 * @see IJobManager#getPriority
	 * @see IJobManager#setPriority
	 * @see Job#run
	 */
	public static final int INTERACTIVE = 10;
	/** 
	 * Job priority constant (value 20) for short background jobs.
	 * Short background jobs are jobs that typically complete within a second,
	 * but may take longer in some cases.  Short jobs are given priority
	 * over all other jobs except interactive jobs.
	 * 
	 * @see IJobManager#getPriority
	 * @see IJobManager#setPriority
	 * @see Job#run
	 */
	public static final int SHORT = 20;
	/** 
	 * Job priority constant (value 30) for long-running background jobs.
	 * 
	 * see IJobManager#getPriority
	 * @see IJobManager#setPriority
	 * @see Job#run
	 */
	public static final int LONG = 30;

	/** 
	 * Job priority constant (value 40) for build jobs.  Build jobs are
	 * generally run after all other background jobs complete.
	 * 
	 * @see IJobManager#getPriority
	 * @see IJobManager#setPriority
	 * @see Job#run
	 */
	public static final int BUILD = 40;

	/** 
	 * Job priority constant (value 50) for decoration jobs.
	 * Decoration jobs have lowest priority.  Decoration jobs generally
	 * compute extra information that the user may be interested in seeing
	 * but is generally not waiting for.
	 * 
	 * @see IJobManager#getPriority
	 * @see IJobManager#setPriority
	 * @see Job#run
	 */
	public static final int DECORATE = 50;
	/** 
	 * Job return code (value 0) indicating successful completion.
	 * 
	 * @see Job#run
	 */
	public static final int SUCCESS = 0;
	/** 
	 * Job return code (value 1) indicating failed completion.
	 * 
	 * @see Job#run
	 */
	public static final int FAILURE = 1;
	/**
	 * Job return code (value 2) indicating that a job was abnormally
	 * canceled.
	 * 
	 * @see Job#run
	 */
	public static final int CANCELED = 2;

	/** 
	 * Job state/priority code (value 4).  Indicates that a job is not paused, 
	 * waiting, or running (i.e., the job manager doesn't know anything about
	 * the job).
	 * 
	 * @see IJobManager#getPriority
	 * @see IJobManager#getState
	 */
	public static final int NONE = -1;

	/** 
	 * Job return/state code (value 3) indicating that a job is paused.
	 * 
	 * @see Job#run
	 * @see IJobManager#getState
	 */
	public static final int PAUSED = 3;

	/** 
	 * Job state code (value 4) indicating that a job is waiting to be run.
	 * 
	 * @see IJobManager#getState
	 */
	public static final int WAITING = 4;

	/** 
	 * Job state code (value 5) indicating that a job is currently running
	 * 
	 * @see IJobManager#getState
	 */
	public static final int RUNNING = 5;

	/**
	 * Stops the job.  If the job is currently waiting,
	 * it will be removed from the queue.  If the job is paused,
	 * it will be discarded without having a chance to resume and its paused state
	 * will be cleared.  If the job is currently executing, it will be asked to
	 * stop but there is no guarantee that it will do so.
	 * 
	 * @return false if the job is currently running (and thus may not
	 * respond to cancelation), and true in all other cases.
	 */
	public final boolean cancel() {
		return super.cancel();
	}

	/**
	 * Returns the priority of a job that is waiting, running, or
	 * paused.  Returns NONE if the job is not in any of these states.
	 * 
	 * @return the priority of the job.  One of INTERACTIVE, SHORT, LONG, BUILD, 
	 * 	DECORATE, or NONE.
	 */
	public final int getPriority() {
		return super.getPriority();
	}
	/**
	 * Returns the state of the job. Result will be one of:
	 * <ul>
	 * <li>Job.RUNNING - if the job is currently being run.</li>
	 * <li>Job.WAITING - if the job is waiting to be run.</li>
	 * <li>Job.PAUSED - if the job is paused.</li>
	 * <li>Job.NONE - in all other cases.</li>
	 * </ul>
	 * Return the job state
	 */
	public final int getState() {
		return super.getState();
	}
	/**
	 * Requests that this job be suspended.
	 * If the job is currently waiting to be run, it will be removed
	 * from the queue.  If the job is currently running, this method has no effect. If
	 * the job is not currently running, its state will become <code>PAUSED</code>,
	 * and will remain that way until either resumed or canceled.
	 * 
	 * Paused jobs can be resumed using <code>resume</code>.
	 * 
	 * @return false if the job is currently running (and thus cannot
	 * be paused), and true in all other cases.
	 */
	public final boolean pause() {
		return super.pause();
	}
	/**
	 * Resumes execution of the given job.  If the job is not currently
	 * paused, this request is ignored.
	 * @param job
	 */
	public final void resume() {
		super.resume();
	}
	/**
	 * Executes the current job.  Returns the result of the execution.
	 * 
	 * The provided monitor can be used to report progress and respond to 
	 * cancellation.  If the progress monitor has been cancelled, the job
	 * should finish its execution at the earliest convenience. 
	 * 
	 * Once a job is stopped, it will not be asked to run again unless explicitly
	 * rescheduled.
	 * @return the job result.
	 */
	public abstract IStatus run(IProgressMonitor monitor);
	/**
	 * Sets the priority of the job.  This will not affect the execution of
	 * a running job, but it will affect how the job is scheduled while
	 * it is waiting to be run.
	 * 
	 * @param priority the new job priority.  One of
	 * INTERACTIVE, SHORT, LONG, BUILD, or DECORATE.
	 */
	public final void setPriority(int i) {
		super.setPriority(i);
	}

	/**
	 * Returns true if the job should be run, and false otherwise.
	 * If false is returned, this job will be discard by the job manager
	 * and never be run (unless explictly rescheduled).
	 * 
	 * <p>This method will be called immediately prior to calling the job's
	 * run method, so it can be used for last minute pre-condition checking before
	 * a job is run.  </p>
	 */
	public boolean shouldRun() {
		return true;
	}
}