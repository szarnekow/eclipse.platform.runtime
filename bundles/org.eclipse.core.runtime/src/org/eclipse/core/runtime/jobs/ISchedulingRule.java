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
 * Scheduling rules are used by jobs to indicate when they need exclusive access
 * to a resource.  The job manager guarantees that no two jobs with conflicting
 * scheduling rules will run concurrently.  How the rules are defined and composed
 * is entirely up to clients of the job manager.
 * 
 * @see Job#getRule
 * @see Job#setRule
 * @since 3.0
 */
public interface ISchedulingRule {
	/**
	 * Returns whether this scheduling rule is compatible with another scheduling rule.
	 * If <code>false</code> is returned, then no job with this rule will be run at the 
	 * same time as a job with the conflicting rule.  If <code>true</code> is returned, 
	 * then the job manager is free to run jobs with these rules at the same time.
	 * 
	 * @param rule the rule to check for conflicts
	 * @return <code>true</code> if the rule is conflicting, and <code>false</code>
	 * 	otherwise.
	 */
	public boolean isConflicting(ISchedulingRule rule);
}
