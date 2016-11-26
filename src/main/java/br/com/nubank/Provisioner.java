package br.com.nubank;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
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
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.util.EC2MetadataUtils;

import br.com.nubank.database.DBConnection;
import br.com.nubank.listener.UpdateListener;
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
    	
    	AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard().withRegion(System.getenv("REGION")).build();
    	
    	DescribeInstancesResult result = ec2.describeInstances();
    	
    	List<Reservation> reservations = result.getReservations();

        List<Instance> instances;
        for(Reservation res : reservations){
        	instances = res.getInstances();
            for(Instance ins : instances){
            	if (ins.getImageId().equals(EC2MetadataUtils.getInstanceId())) {
            		jsonObject.put("PROVISIONER_IP", ins.getPublicIpAddress());
            	}
            }
        }
    	
    	AWSCredentials credentials = new EnvironmentVariableCredentialsProvider().getCredentials();
		AmazonSQS sqs = new AmazonSQSClient(credentials);
		
		logger.info("Sending message to queue sqs_launch");
		sqs.sendMessage(new SendMessageRequest(System.getenv("SQS_LAUNCH_URL"), scheduler));
    	
		nuDb.insertJob("", "", jsonObject.getJSONObject("job").getString("schedule"), "requested");
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
        
    	AWSCredentials credentials = new EnvironmentVariableCredentialsProvider().getCredentials();
		AmazonSQS sqs = new AmazonSQSClient(credentials);
		
		Job job = nuDb.queryJob(instanceId);
		
		JSONObject jsonObject = new JSONObject(job);
    	String stringJob = jsonObject.toString();
		
		logger.info("Sending message to queue sqs_destroy");
		sqs.sendMessage(new SendMessageRequest(System.getenv("SQS_DESTROY_URL"), stringJob));
		
		job.setStatus("done");
    	logger.info("Job is done, sending message to queue sqs_update");
    	sqs.sendMessage(new SendMessageRequest(System.getenv("SQS_UPDATE_URL"), stringJob));

    }

    public static void main(String[] args) throws Exception {
    	
        SpringApplication.run(Provisioner.class, args);
        nuDb.createJobTable();
        UpdateListener update = new UpdateListener();
        update.updateJob(nuDb);
        
    }

}