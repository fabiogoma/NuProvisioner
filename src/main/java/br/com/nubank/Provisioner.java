package br.com.nubank;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.SendMessageRequest;

import br.com.nubank.database.DBConnection;
import br.com.nubank.helpers.UpdateListener;
import br.com.nubank.pojos.Job;


@RestController
@EnableAutoConfiguration
public class Provisioner {
	private final static DBConnection nuDb = new DBConnection();
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @RequestMapping(value = "/schedule/", method = RequestMethod.POST)
    @ResponseBody
    public void schedule(@RequestBody Map<String, Object> payload) throws JSONException, ParseException{

    	JSONObject jsonObject = new JSONObject(payload);
    	String scheduler = jsonObject.toString();
    	
    	AWSCredentials credentials = new ProfileCredentialsProvider().getCredentials();
		AmazonSQS sqs = new AmazonSQSClient(credentials);
		
		logger.info("Sending message to queue sqs_launch");
		sqs.sendMessage(new SendMessageRequest("https://us-west-2.queue.amazonaws.com/678982507510/sqs_launch", scheduler));
    	
		nuDb.insertJob("", jsonObject.getJSONObject("job").getString("schedule"), "requested");
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ArrayList<Job> list() {
    	
    	logger.info("Listing all jobs");
    	ArrayList<Job> Jobs = nuDb.listAll();
        return Jobs;
  
    }

    @RequestMapping(value = "/status/{instanceId}", method = RequestMethod.GET, produces = "application/json")
    public Job status(@PathVariable("instanceId") String instanceId) {
        
    	logger.info("List one specific job");
    	Job job = nuDb.queryJob(instanceId);
    	return job;
    	
    }

    @RequestMapping(value = "/callback/{instanceId}", method = RequestMethod.DELETE)
    public void callback(@PathVariable("instanceId") String instanceId) {
        
    	AWSCredentials credentials = new ProfileCredentialsProvider().getCredentials();
		AmazonSQS sqs = new AmazonSQSClient(credentials);
		
		logger.info("Sending message to queue sqs_destroy");
		sqs.sendMessage(new SendMessageRequest("https://us-west-2.queue.amazonaws.com/678982507510/sqs_destroy", instanceId));
		
		Job job = nuDb.queryJob(instanceId);
    	job.setStatus("done");
    	
    	JSONObject jsonObject = new JSONObject(job);
    	String stringJob  =jsonObject.toString();

    	logger.info("Job is done, sending message to queue sqs_update");
    	sqs.sendMessage(new SendMessageRequest("https://us-west-2.queue.amazonaws.com/678982507510/sqs_update", stringJob));

    }

    public static void main(String[] args) throws Exception {
    	
        SpringApplication.run(Provisioner.class, args);
        nuDb.createJobTable();
        UpdateListener update = new UpdateListener();
        update.updateJob(nuDb);
        
    }

}