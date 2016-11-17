package br.com.nubank.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.nubank.pojos.Job;

public class DBConnection {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private static final String DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
	private static final String JDBC_URL = "jdbc:derby:nudatabase;create=true";
	
	Connection conn;
	
	public DBConnection() {
		try {
			this.conn = DriverManager.getConnection(JDBC_URL);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if (this.conn != null){
			logger.info("Connected to the database");
		}

	}
	
	public void createJobTable(){
		try {
			DatabaseMetaData dbmd = this.conn.getMetaData();
			ResultSet rs = dbmd.getTables(null, "APP", "JOBSTATUS", null);
			if(!rs.next()){
				conn.createStatement().execute("CREATE TABLE jobStatus(InstanceId varchar(50), Schedule varchar(50), Status varchar(20))");
				logger.info("Table created");
			}else{
				conn.createStatement().execute("TRUNCATE TABLE jobStatus");
				logger.info("Table truncated");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
	
	public void insertJob(String InstanceId, String Schedule, String Status){
		try {
			conn.createStatement().execute("INSERT INTO jobStatus VALUES('" + InstanceId + "', '" + Schedule + "', '" + Status + "')");
			logger.info("Inserted data");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void updateJob(String InstanceId, String Schedule, String Status){
		try {
			conn.createStatement().execute("UPDATE jobStatus SET InstanceId='" + InstanceId + "', Status='" + Status + "' WHERE Schedule='" + Schedule + "'");
			logger.info("Updated data");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public Job queryJob(String instanceID) {

		Job job = new Job();
		
		try {
			Statement st = this.conn.createStatement();
		
			ResultSet rs = st.executeQuery("SELECT * FROM jobStatus WHERE InstanceId='" + instanceID + "'");
	
			rs.next();
	
			String instanceId = rs.getString("InstanceId");
			String schedule = rs.getString("Schedule");
			String status = rs.getString("Status");
			
			job.setInstanceId(instanceId);
			job.setSchedule(schedule);
			job.setStatus(status);
			logger.info("Query executed");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return job;
	}
	
	public ArrayList<Job> listAll() {
		
		ArrayList<Job> Jobs = new ArrayList<Job>();
		
		try {
			Statement st = this.conn.createStatement();
		
			ResultSet rs = st.executeQuery("SELECT * FROM jobStatus");
			
			while(rs.next()){
				String instanceId = rs.getString("InstanceId");
				String schedule = rs.getString("Schedule");
				String status = rs.getString("Status");
				
				Job job = new Job();
				
				job.setInstanceId(instanceId);
				job.setSchedule(schedule);
				job.setStatus(status);
				
				Jobs.add(job);
			}
			logger.info("Listing all jobs");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return Jobs;
	}
}
