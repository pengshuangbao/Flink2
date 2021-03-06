package com.dfheinz.flink.beans;

import java.util.ArrayList;
import java.util.List;

import com.dfheinz.flink.utils.Utils;

public class ProcessedSumWindow {

	// Data
	private long windowStart;
	private long windowEnd;
	private List<EventBean> events = new ArrayList<EventBean>();
	private long computedSum;
	
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		for (EventBean eventBean : events) {
		  String line = String.format("WindowStart=%s WindowEnd=%s Event=%s Value=%s ComputedSum=%d\n", 
				                       getTS(windowStart), getTS(windowEnd), eventBean.getLabel(), eventBean.getValue(), computedSum);
		  buffer.append(line);
		}
		return buffer.toString();
	}	

	
	private String getTS(long timestamp) {
		return Utils.getFormattedTimestamp(timestamp);
	}
	
	
	public long getWindowStart() {
		return windowStart;
	}
	public void setWindowStart(long windowStart) {
		this.windowStart = windowStart;
	}
	public long getWindowEnd() {
		return windowEnd;
	}
	public void setWindowEnd(long windowEnd) {
		this.windowEnd = windowEnd;
	}
	public List<EventBean> getEvents() {
		return events;
	}
	public void setEvents(List<EventBean> events) {
		this.events = events;
	}

	public long getComputedSum() {
		return computedSum;
	}
	public void setComputedSum(long computedSum) {
		this.computedSum = computedSum;
	}
	
	
	
}
