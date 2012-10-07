/*******************************************************************************
 * Copyright 2011 Patrick McMorran
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package datagenerator.generators;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Random;

import datagenerator.util.QueryGenerator;

/**
 * This class generates random integers within a given range.
 * @author Patrick McMorran
 *
 */
public class NumberIntGenerator implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int Minimum;
	private int Maximum;
	Random R;
	private boolean unique;
    private String temp_table;
    static int generatorID = 999999;
    //////////////Database
    private Connection conn;
    private Statement s = null;
    private ResultSet rs = null;
    private PreparedStatement psInsert = null;
	
    /**
     * This is the Constructor for the Generator.
     * @param Min This is an integer representing the lower bound of the number range that can be generated.
	 * @param Max This is an integer representing the upper bound of the number range that can be generated.
	 * @param u This is a boolean value that denotes whether the generator should generate unique values. 
	 * true = unique, false = nonunique;
     * 
     */
	public NumberIntGenerator(int Min, int Max, boolean u) {
		R = new Random();
		this.Minimum = Min;
		this.Maximum = Max - Min;
		this.Maximum++;
		unique = u;
		this.temp_table = "temp_int_" + generatorID;
		generatorID--;
	}
	
	/**
	 * This is used to prepare the generator, creating required tables in the database, loading required
	 * data into memory, and handling any other dependencies.
	 * @param c This is the connection object to the database that the generator will use.
	 */
	public void prepareGenerator(Connection c){
		this.conn = c;
		try{
		s = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
					ResultSet.CONCUR_UPDATABLE);
		try{
			//This cleans up a previously messy operation
		s.execute("Drop Table " + this.temp_table);
		}catch(Throwable R){
			
		}
		s.execute("create table " + this.temp_table + "(string varchar(80))");
		psInsert = conn.prepareStatement("insert into " + this.temp_table + " values (?)");
		
		}catch(Throwable R){
			R.printStackTrace();
			System.exit(-1);
		}
		
	}
	
	/**
	 * This performs cleanup operations, deleting temporary tables and indices. 
	 */
	public void destroyGenerator(){
		try{
			conn.commit();
			String Q = "Drop Table " +  this.temp_table;
			this.s.execute(Q);
		}catch(Throwable r){
			r.printStackTrace();
			System.exit(-1);			
		}
	}
	
	/**
	 * This method returns a random integer within the specifications of the generator formatted as a string.
	 * @return This string contains the randomly selected integer.
	 */
	public String nextTuple(){
		String Tuple = "";
		int I = R.nextInt(Maximum);
		I = I + Minimum;
		Tuple += I;
		if(this.unique){
			String q = QueryGenerator.getCharStringUniqueQuery(Tuple, temp_table);
			int NumberofResults = 1;
			try{
			rs = s.executeQuery(q);
			rs.last();//Go to last row in the results
			NumberofResults = rs.getRow();//Get the row ID, so that we know how many rows there are
			rs.first();
			}catch(Throwable R){
				//Query Failed
				R.printStackTrace();
				System.exit(0);
			}
			if(NumberofResults != 0){
				//This is not unique!
				return nextTuple();
			}else{
				try{
				psInsert.setString(1, Tuple);
				psInsert.executeUpdate();
				return Tuple;//if this is unique
				}catch(Throwable R){
					R.printStackTrace();
					System.exit(-1);
				}
			}
		}
		return Tuple;
	}
	
}
