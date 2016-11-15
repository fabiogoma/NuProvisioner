package br.com.nubank;

import java.text.ParseException;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
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


@RestController
@EnableAutoConfiguration
public class Provisioner {

    @RequestMapping(value = "/schedule/", method = RequestMethod.POST)
    @ResponseBody
    public void schedule(@RequestBody Map<String, Object> payload) throws JSONException, ParseException{

    	JSONObject jsonObject = new JSONObject(payload);
    	String scheduler = jsonObject.toString();
    	
    	AWSCredentials credentials = new ProfileCredentialsProvider().getCredentials();
		AmazonSQS sqs = new AmazonSQSClient(credentials);
		
		System.out.println("Sending message");
		sqs.sendMessage(new SendMessageRequest("https://us-west-2.queue.amazonaws.com/678982507510/sqs_launch", scheduler));
    	
		/*
    	Scheduler sched = ScheduleParser.JsonToObject(payload);    	
    	System.out.println(sched.getImage());
    	System.out.println(sched.getSchedule());
    	
    	
    	Map<String, String> map = sched.getVariables();
		for (Map.Entry<String, String> entry : map.entrySet())
		{
		    System.out.println(entry.getKey() + "=" + entry.getValue());
		}
		*/
    }

    @RequestMapping("/list")
    String list() {
        return "Hello list!";
    }

    @RequestMapping(value = "/status/{jobId}", method = RequestMethod.GET)
    private String status(@PathVariable("jobId") Integer jobId) {
        return "Status of " + jobId.toString();
    }

    @RequestMapping("/callback")
    String callback() {
        return "Hello callback!";
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Provisioner.class, args);
    }

}