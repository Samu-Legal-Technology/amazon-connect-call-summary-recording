package com.ott.connect;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.PutRecordResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent.KinesisEventRecord;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@SpringBootApplication
public class KinesisVideoStreamingHandler implements RequestHandler<KinesisEvent, String> {
	
	private static Logger logger = LoggerFactory.getLogger(KinesisVideoStreamingHandler.class);
	
	Gson gson = new GsonBuilder().setPrettyPrinting().create();
	public final SimpleDateFormat dateFormate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public static final Supplier<AmazonDynamoDB> dynamoDBClient = () -> AmazonDynamoDBClientBuilder.standard().build();

	public static final Consumer<KinesisEventRecord> pushDataToVoiceMailKinesisStream= kinesisEventRecord -> {
		AmazonKinesisClientBuilder clientBuilder = AmazonKinesisClientBuilder.standard();

		clientBuilder.setRegion("us-east-1");


		AmazonKinesis kinesisClient = clientBuilder.build();

		PutRecordRequest putRecordRequest = new PutRecordRequest();
		logger.info("Kinesis Stream : {}",System.getenv("DESTINATION_KINESIS_STREAM"));
		putRecordRequest.setStreamName(System.getenv("DESTINATION_KINESIS_STREAM"));
		putRecordRequest.setPartitionKey(String.format("partitionKey-%d", new Random().nextInt()));
		putRecordRequest.setData(ByteBuffer.wrap(new String(kinesisEventRecord.getKinesis().getData().array()).getBytes()));
		PutRecordResult putRecordResult = kinesisClient.putRecord(putRecordRequest);
		logger.info("Kinesis Put Result {}" , putRecordResult);
		
	};

	@Override
	public String handleRequest(KinesisEvent event, Context context) {
		LambdaLogger lambdaLogger = context.getLogger();
		lambdaLogger.log("Lambda invokation started ..............");
		try {

			lambdaLogger.log("RAW CONTEXT: " + context);
			lambdaLogger.log("RAW EVENT: " + event);

			lambdaLogger.log("CONTEXT: " + gson.toJson(context));
			// process event
			lambdaLogger.log("EVENT: " + gson.toJson(event));

			lambdaLogger.log("Processing Kinesis Records ...........");
			for (KinesisEventRecord rec : event.getRecords()) {
				lambdaLogger.log(new String(rec.getKinesis().getData().array()));
				String kinesisRecord = new String(rec.getKinesis().getData().array());

				JSONObject jsonObject = new JSONObject(kinesisRecord);
				JSONObject recordingJsonObj = jsonObject.get("Recording") == JSONObject.NULL ? null
						: (JSONObject) jsonObject.get("Recording");
				JSONObject systemEndpointJsonObj = (JSONObject) jsonObject.get("SystemEndpoint");
				JSONObject customerEndpointJsonObj = (JSONObject) jsonObject.get("CustomerEndpoint");
				JSONObject agentJsonObj = (jsonObject.get("Agent") == JSONObject.NULL) ? null
						: (JSONObject) jsonObject.get("Agent");

				DynamoDB db = new DynamoDB(dynamoDBClient.get());

				Table table = db.getTable("OttConnectContactTraceRecord-Dev");

				Item item = table.getItem("ContactId", jsonObject.get("ContactId").toString());
				logger.info("Is item available ? : {}" , (item != null));

				if (item == null) {
					item = new Item().withPrimaryKey("ContactId", jsonObject.get("ContactId").toString())
//						.withPrimaryKey("SummaryId", customerEndpointJsonObj.getString("Address")+"-"+jsonObject.get("ContactId").toString())
							.withString("CustomerContactNumber", customerEndpointJsonObj.getString("Address"))
							.withString("AgentContactNumber",
									(systemEndpointJsonObj != null) ? systemEndpointJsonObj.getString("Address") : "")
							.withString("InitiationMethod", jsonObject.getString("InitiationMethod"))
							.withString("Channel", jsonObject.get("Channel").toString())
							.with("CustomerContactDetails", customerEndpointJsonObj.toString())
							.with("SystemContactDetails", systemEndpointJsonObj.toString())
							.withString("AgentUsername",
									(agentJsonObj == null ? "" : agentJsonObj.getString("Username")))
							.with("S3RecordingKey", (recordingJsonObj == null ? "" : recordingJsonObj.get("Location")))
							.with("S3RecordingURL",
									"https://" + (recordingJsonObj == null ? "" : recordingJsonObj.get("Location")))
							.withString("InitiationTimestamp", jsonObject.getString("InitiationTimestamp"))
							.withString("DisconnectTimestamp", jsonObject.getString("DisconnectTimestamp"))
							.withString("Status", "Pending").withString("CreatedAt", dateFormate.format(new Date()));

					PutItemOutcome putItem = table.putItem(item);
					if (putItem.getPutItemResult().getSdkHttpMetadata().getHttpStatusCode() == 200)
						lambdaLogger.log("Data saved in db");
				} else {
					lambdaLogger.log("Item(" + item.getString("ContactId") + ") already exists in DynamoDB.........");
				}
				
				pushDataToVoiceMailKinesisStream.accept(rec);
			}

		} catch (Exception e) {
			e.printStackTrace();
			lambdaLogger.log(e.getMessage());
		}

		return "{ \"result\": \"Success\" }";

	}

}
