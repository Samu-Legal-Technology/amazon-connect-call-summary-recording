package com.ott.connect;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ConnectEvent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@SpringBootApplication
public class ConnectLambdaHandler implements RequestHandler<ConnectEvent, String> {
	Gson gson = new GsonBuilder().setPrettyPrinting().create();
	public final SimpleDateFormat dateFormate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public final Supplier<AmazonDynamoDB> dynamoDBClient = () -> AmazonDynamoDBClientBuilder.standard().build();


	@Override
	public String handleRequest(ConnectEvent event, Context context) {
		LambdaLogger logger = context.getLogger();
		logger.log("Lambda invokation started ..............");
		try {

			logger.log("RAW CONTEXT: " + context);
			logger.log("RAW EVENT: " + event);

			logger.log("CONTEXT: " + gson.toJson(context));
			// process event
			logger.log("EVENT: " + gson.toJson(event));

//		Add entry in dynamodb
			DynamoDB db = new DynamoDB(dynamoDBClient.get());

			Table table = db.getTable("dev-ott-connect-contact-details");
			Item item = new Item().withPrimaryKey("contact_id", event.getDetails().getContactData().getContactId())
					.with("channel", event.getDetails().getContactData().getChannel())
					.withJSON("customer_contact_details",
							gson.toJson(event.getDetails().getContactData().getCustomerEndpoint()))
					.withJSON("system_contact_details",
							gson.toJson(event.getDetails().getContactData().getSystemEndpoint()))
					.withString("CreatedAt", dateFormate.format(new Date()));

			PutItemOutcome putItem = table.putItem(item);
			if (putItem.getPutItemResult().getSdkHttpMetadata().getHttpStatusCode() == 200)
				logger.log("Data saved in db");

		} catch (Exception e) {
			e.printStackTrace();
			logger.log(e.getMessage());
		}

		Map<String, String> resultMap = new HashMap<>();
		resultMap.put("Name", "Test Name");
		resultMap.put("Address", "Test Address");
		resultMap.put("CallerType", "Test Call");
		return resultMap.toString();

	}
}
