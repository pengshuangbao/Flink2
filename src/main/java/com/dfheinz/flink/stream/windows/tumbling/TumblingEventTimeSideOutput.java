package com.dfheinz.flink.stream.windows.tumbling;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import com.dfheinz.flink.beans.EventBean;
import com.dfheinz.flink.beans.ProcessedSumWindow;
import com.dfheinz.flink.utils.Utils;


public class TumblingEventTimeSideOutput {
	
	private static final OutputTag<EventBean> lateEventsTag = new OutputTag<EventBean>("late-events") {};

	public static void main(String[] args) throws Exception {
			
		// Step 1: Get Execution Environment
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
		env.setParallelism(1);
		String host = "localhost";
		int port = 9999;
		
		// Step 2: Get Data
		DataStream<EventBean> eventStream = env
				.socketTextStream(host, port)
				.map(new EventBeanParser())
				.assignTimestampsAndWatermarks(new EventBeanTimestampAssignerWithRetractedWatermark(Time.seconds(0)));
		
		// Step 3: Perform Transformations and Operations
		SingleOutputStreamOperator<ProcessedSumWindow> processedWindows = eventStream
				.keyBy("key")
				.window(TumblingEventTimeWindows.of(Time.seconds(2)))
				.sideOutputLateData(lateEventsTag)
				.process(new MyProcessFunction());
		
		// Get Late Events Stream
		DataStream<EventBean> lateEvents = processedWindows.getSideOutput(lateEventsTag);
		
		// Step 4: Write to Sink(s)
		processedWindows.writeAsText("output/tumbling_event_time.txt",FileSystem.WriteMode.OVERWRITE).setParallelism(1);
		lateEvents.writeAsText("output/tumbling_event_time_late_data.txt",FileSystem.WriteMode.OVERWRITE).setParallelism(1);

		// Step 5: Trigger Execution
		env.execute("TumblingEventTimeSideOutput");
	}
	
	
	private static class EventBeanTimestampAssignerWithRetractedWatermark implements AssignerWithPeriodicWatermarks<EventBean> {	
		private long retractAmount = 0;
		private long currentMaxTimestamp = 0;
		private long previousWatermark = -1;
		
		public EventBeanTimestampAssignerWithRetractedWatermark(Time maxLateness) {
			retractAmount = maxLateness.toMilliseconds();
		}
		public EventBeanTimestampAssignerWithRetractedWatermark() {
		}
		
	    @Override
	    public long extractTimestamp(EventBean element, long previousElementTimestamp) {
	        long timestamp = element.getTimestamp();
	        System.out.println("Event: " + element.getLabel() + " timestamp=" + Utils.getFormattedTimestamp(timestamp));
	        currentMaxTimestamp = Math.max(timestamp, currentMaxTimestamp);
	        return timestamp;
	    }

	    @Override
	    public Watermark getCurrentWatermark() {    	
	    	// Retract the watermark.
	    	long watermark = currentMaxTimestamp - retractAmount;
	    	// Output watermark if it has changed
	    	if (currentMaxTimestamp != previousWatermark) {
	    		previousWatermark = currentMaxTimestamp;
	    		System.out.println("Adjusted Watermark=" + Utils.getFormattedTimestamp(watermark));
	    	} 
	    	return new Watermark(watermark);
	    }
	   
	}


	
	// Generic Parameters: Input, Output, Key, Window
	private static class MyProcessFunction extends ProcessWindowFunction<EventBean, ProcessedSumWindow, Tuple, TimeWindow> {
		
		// Function Parameters: Key, Context, Input Elements, Output Collector
		@Override
		public void process(Tuple key,Context context,Iterable<EventBean> inputElements, Collector<ProcessedSumWindow> collector) throws Exception {		
			System.out.println("PROCESSING WINDOW BEGIN " + Utils.getFormattedTimestamp(context.window().getStart()));
			ProcessedSumWindow processedSumWindow = new ProcessedSumWindow();
			processedSumWindow.setWindowStart(context.window().getStart());
			processedSumWindow.setWindowEnd(context.window().getEnd());
			long computedSum = 0;
			for (EventBean nextEvent : inputElements) {
				System.out.println(nextEvent.getLabel() + " " + Utils.getFormattedTimestamp(nextEvent.getTimestamp()));
				processedSumWindow.getEvents().add(nextEvent);
				computedSum += Long.valueOf(nextEvent.getValue());
			}
			processedSumWindow.setComputedSum(computedSum);
			collector.collect(processedSumWindow);
			System.out.println("PROCESSING WINDOW END " + Utils.getFormattedTimestamp(context.window().getEnd()));
		}
	}
	
	
	private static class EventBeanParser implements MapFunction<String,EventBean> {	
		public EventBean map(String input) throws Exception {
			String[] tokens = input.split(",");;
			EventBean event = new EventBean();
			event.setKey(tokens[0]);
			event.setLabel(tokens[1]);
			event.setValue(tokens[2]);
			event.setTimestamp(Long.parseLong(tokens[3]));
			event.setProcessTime(System.currentTimeMillis());
			return event;
		}
	}
	
	
}
