/*
Copyright 2008,2009 Complex Automata Simulation Technique (COAST) consortium

GNU Lesser General Public License

This file is part of MUSCLE (Multiscale Coupling Library and Environment).

    MUSCLE is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    MUSCLE is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with MUSCLE.  If not, see <http://www.gnu.org/licenses/>.
*/

package utilities;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;


/**
helper class to measure cpu/wallclock times for the jvm or single threads
@author Jan Hegewald
*/
public class Timing {


	/**
	thread CPU time in nanoseconds (user+system)
	*/
	public static long getThreadCpuTime() {

		ThreadMXBean tBean = ManagementFactory.getThreadMXBean( );
		return tBean.isCurrentThreadCpuTimeSupported( ) ? tBean.getCurrentThreadCpuTime() : 0L;
	}


	/**
	thread user time in nanoseconds
	*/
	public static long getThreadUserTime() {

		ThreadMXBean tBean = ManagementFactory.getThreadMXBean( );
		return tBean.isCurrentThreadCpuTimeSupported( ) ? tBean.getCurrentThreadUserTime() : 0L;
	}


	/**
	thread system time in nanoseconds
	*/
	public static long getThreadSystemTime() {

		ThreadMXBean tBean = ManagementFactory.getThreadMXBean( );
		return tBean.isCurrentThreadCpuTimeSupported() ? (tBean.getCurrentThreadCpuTime() - tBean.getCurrentThreadUserTime()) : 0L;
	}


	/**
	JVM CPU time in nanoseconds
	*/
	public static long getJVMCpuTime() {
		OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
		if ( !(osBean instanceof com.sun.management.OperatingSystemMXBean) ) {
			throw new java.lang.UnsupportedOperationException("can not determine cpu time because there is not a <com.sun.management.OperatingSystemMXBean>, but a <"+osBean.getClass()+">");
		}

		return ((com.sun.management.OperatingSystemMXBean)osBean).getProcessCpuTime();
	}


	//
	private long threadStartTime;
	private long totalStartTime;
	private boolean isCounting;
	private long threadTime;
	private long totalTime;

	//
	public Timing() {

		this.threadStartTime = Timing.getThreadCpuTime();
//		totalStartTime = getJVMCpuTime();
		this.totalStartTime = 0;
		this.isCounting = true;
	}


	//
	public void stop() {

		if( this.isCounting ) {
			this.threadTime = Timing.getThreadCpuTime() - this.threadStartTime;
			this.totalTime = Timing.getJVMCpuTime() - this.totalStartTime;
			this.isCounting = false;
		}
	}


	//
	public boolean isCounting() {

		return this.isCounting;
	}

	//
	@Override
	public String toString() {

		this.stop();
		float percent = 100.0f;
		if( this.totalTime > 0 ) {
			percent = this.threadTime/(float)this.totalTime*100.0f;
		}
		return (this.threadTime)/1000000000.0f+" s /"+(this.totalTime)/1000000000.0f+" s = "+percent+" %";
	}


	//
	public static void main(String[] args) {

		Timing t = new Timing();
		System.out.println(t);
		for(int i = 0; i< 10000042; i++) {
		}
		System.out.println(t);
	}
}