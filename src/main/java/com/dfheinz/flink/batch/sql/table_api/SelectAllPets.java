package com.dfheinz.flink.batch.sql.table_api;

import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.common.typeinfo.TypeInformation;
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

public class SelectAllPets {
	
	public static void main(String[] args) throws Exception {
		try {
			// Step 1: Get Execution Environment
			ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
			BatchTableEnvironment tableEnv = TableEnvironment.getTableEnvironment(env);
			int parallelism = 1;
			ParameterTool parms = ParameterTool.fromArgs(args);
			env.getConfig().setGlobalJobParameters(parms);
						
			// id,species,breed,dob,name,weight
			// 311,canine,golden retriever,10/22/2008,Captain,75.00
			
			// Step 2: Get Table Source
			CsvTableSource orderTableSource = CsvTableSource.builder()
					.path("input/batch/pets.csv")
				    .ignoreFirstLine()
				    .fieldDelimiter(",")
				    .field("id", Types.LONG())
				    .field("species", Types.STRING())
				    .field("breed", Types.STRING())
				    .field("date_of_birth", Types.SQL_DATE())
				    .field("name", Types.STRING())
				    .field("weight", Types.DOUBLE())
				    .build();		
			
			// Step 3: Register our table source
			tableEnv.registerTableSource("pets", orderTableSource);
			Table petTable = tableEnv.scan("pets");
			
			// Step 4: Perform Operations
			// SELECT *
			// FROM pets
			Table allPets = petTable
				.select("id,species,breed,date_of_birth,name,weight");
								
			// Step 5: Write Results to Sink
			TableSink<Row> sink = new CsvTableSink("output/select_all_pets.csv", ",", parallelism, WriteMode.OVERWRITE);
			allPets.writeToSink(sink);
					
			// Step 6: Trigger Application Execution
			JobExecutionResult result  =  env.execute("SelectAllPets");
		
		} catch (Exception e) {
			System.out.println("ERROR:\n" + e);
		}
	}
}
