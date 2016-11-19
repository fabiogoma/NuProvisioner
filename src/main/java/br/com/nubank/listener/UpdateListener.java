package br.com.nubank.listener;

import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;

import br.com.nubank.database.DBConnection;
import br.com.nubank.pojos.Job;

public class UpdateListener {
	private static Logger logger = Logger.getLogger(UpdateListener.class);
	
	public void updateJob(DBConnection db){
		AWSCredentials credentials = new ProfileCredentialsProvider().getCredentials();
		AmazonSQS sqs = new AmazonSQSClient(credentials);
		
		logger.info("Receiving messages from sqs_update");
		String myQueueUrl = "https://us-west-2.queue.amazonaws.com/678982507510/sqs_update";
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(myQueueUrl);
        
        while(true){
        	List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
	        for (Message message : messages) {
	        	JSONObject jsonObject = new JSONObject(message.getBody());
	        	
	        	Job job = new Job();
	        	
	        	job.setInstanceId(jsonObject.getString("instanceId"));
	        	job.setRequestId(jsonObject.getString("requestId"));
	        	job.setSchedule(jsonObject.getString("schedule"));
	        	job.setStatus(jsonObject.getString("status"));
	        	
	         	
	        	db.updateJob(job.getInstanceId(), job.getRequestId(), job.getSchedule(), job.getStatus());
	        		        	
	        	String messageRecieptHandle = messages.get(0).getReceiptHandle();
	        	sqs.deleteMessage(new DeleteMessageRequest(myQueueUrl, messageRecieptHandle));
	        }
	        try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        }

	}

}
