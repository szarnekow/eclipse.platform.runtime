/**********************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.core.tests.harness;

/**
 * The timer class used by performance tests.
 */
class PerformanceTimer {
	private long elapsedTime;
	private long memoryGrowth;
	private String name;
	private long startMemory;
	private long startTime;
	private int repeats = 0;
	/**
	 * 
	 */
	public PerformanceTimer(String name) {
		this.name = name;
		elapsedTime = 0;
		startTime = 0;
		startMemory = 0;
		memoryGrowth = 0;
	}
	public void gc() {
		System.gc();
		System.runFinalization();
		System.gc();
	}
	/**
	 * Return the total elapsed time for all runs.
	 */
	public long getTotalElapsedTime() {
		return elapsedTime;
	}
	/**
	 * Return the average elapsed time over all runs
	 */
	public long getAverageElapsedTime() {
		if (repeats <= 0)
			return 0l;
		return elapsedTime / repeats;
	}
	/**
	 * Return the timer name.
	 */
	public String getName() {
		return name;
	}
	/**
	 * Start the timer.
	 */
	public void startTiming() {
		repeats++;
		gc();
		startTime = System.currentTimeMillis();
		startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
	}
	/**
	 * Stop the timer, add the elapsed time to the total.
	 */
	public void stopTiming() {
		if (startTime == 0)
			return;
		long timeNow = System.currentTimeMillis();
		elapsedTime += (timeNow - startTime);
		startTime = 0;
		
		gc();
		long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		memoryGrowth += (endMemory - startMemory);
		startMemory = 0;
	}
	/**
	 * Returns the overall memory growth for all runs
	 */
	public long getTotalMemoryGrowth() {
		return memoryGrowth;
	}
	/**
	 * Returns the average memory growth for all runs
	 */
	public long getAverageMemoryGrowth() {
		if (repeats <= 0)
			return 0l;
		return memoryGrowth / repeats;
	}
}