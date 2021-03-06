package com.dfheinz.flink.batch.sql.sql_api;

import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.core.fs.FileSystem.WriteMode;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.Types;
import org.apache.flink.table.api.java.BatchTableEnvironment;
import org.apache.flink.table.sinks.CsvTableSink;
import org.apache.flink.table.sinks.TableSink;
import org.apache.flink.table.sources.CsvTableSource;
import org.apache.flink.types.Row;

public class SelectOrdersAndCondition {

	public static void main(String[] args) throws Exception {
		
		try {
	
			// Step 1: Get Execution Environment
			ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
			BatchTableEnvironment tableEnv = TableEnvironment.getTableEnvironment(env); 
			ParameterTool parms = ParameterTool.fromArgs(args);
			env.getConfig().setGlobalJobParameters(parms);
			String input = "input/batch/orders.csv";
			
			// Step 2: Get Table Source
			CsvTableSource orderTableSource = CsvTableSource.builder()
				    .path(input)
				    .ignoreFirstLine()
				    .fieldDelimiter(",")
				    .field("id", Types.LONG())
				    .field("order_date", Types.SQL_DATE())
				    .field("amount", Types.DECIMAL())
				    .field("customer_id", Types.LONG())
				    .build();
			
			
			// Step 3: Register our table source
			tableEnv.registerTableSource("orders", orderTableSource);
			
			// Step 4: Perform Operations
			// SELECT *
			// FROM orders
			// WHERE amount > 35.00
			Table result  = tableEnv.sqlQuery(
				"SELECT id, order_date, amount, customer_id FROM orders WHERE amount > 35.00 AND amount < 100.00");
			
			// Step 5: Write Results to Sink
			int parallelism = 1;
			TableSink<Row> sink = new CsvTableSink("output/select_orders_amount_between_35_and_100.csv", ",", parallelism, WriteMode.OVERWRITE);
			result.writeToSink(sink);
						
			// Step 6: Trigger Application Execution
			JobExecutionResult jobResult  =  env.execute("SelectOrdersAndCondition");

		
		} catch (Exception e) {
			System.out.println("ERROR:\n" + e);
		}
	}
	
}
