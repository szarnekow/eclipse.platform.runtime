/**********************************************************************
 * Copyright (c) 2003 IBM Corporation and others. All rights reserved.   This
 * program and the accompanying materials are made available under the terms of
 * the Common Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.core.tests.harness;

import java.io.*;
import java.io.File;
import java.io.PrintWriter;
import java.util.Iterator;

/**
 * Performance test result that appends timing results to a CSV (comma separated
 * value) file suitable for use by spreadsheet applications.
 */
public class CSVPerformanceTestResult extends PerformanceTestResult {
	private String timingFile;
	private String memoryFile;

	public CSVPerformanceTestResult(String timingFile, String memoryFile) {
		this.timingFile = timingFile;
		this.memoryFile = memoryFile;
	}
	protected void printTimings(PrintWriter out) {
		super.printTimings(out);
		// print out all timing results to the csv files
		try {
			boolean newFiles = !new File(timingFile).exists();
			FileWriter timingOut = new FileWriter(timingFile, true);
			FileWriter memoryOut = new FileWriter(memoryFile, true);
			try {
				if (newFiles)
					writeHeaders(timingOut, memoryOut);
				for (Iterator it = timerList.iterator(); it.hasNext();) {
					PerformanceTimer timer = (PerformanceTimer) it.next();
					timingOut.write(',');
					timingOut.write(Long.toString(timer.getAverageElapsedTime()));
					memoryOut.write(',');
					memoryOut.write(Long.toString(timer.getAverageMemoryGrowth()));
				}
				timingOut.write("\n");
				memoryOut.write("\n");
			} finally {
				timingOut.close();
				memoryOut.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void writeHeaders(FileWriter timingOut, FileWriter memoryOut) throws IOException {
		for (Iterator it = timerList.iterator(); it.hasNext();) {
			PerformanceTimer timer = (PerformanceTimer) it.next();
			timingOut.write(',');
			timingOut.write(timer.getName());
			memoryOut.write(',');
			memoryOut.write(timer.getName());
		}		
		timingOut.write("\n");
		memoryOut.write("\n");
	}
}