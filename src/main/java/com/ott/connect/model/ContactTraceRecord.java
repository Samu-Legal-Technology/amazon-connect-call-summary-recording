package com.ott.connect.model;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class ContactTraceRecord {
	private String awsAccountId;
	private String contactId;
	private CustomerEndpoint customerEndpoint;
	private ContactFlowAttributes attributes;
	private List<KVStreamRecordingData> recordings = new ArrayList<>();

	public ContactTraceRecord(JSONObject jsonObject) {
		this.awsAccountId = jsonObject.getString("AWSAccountId");
		this.contactId = jsonObject.getString("ContactId");
		this.customerEndpoint = new CustomerEndpoint(jsonObject.getJSONObject("CustomerEndpoint"));
		this.attributes = new ContactFlowAttributes(jsonObject.getJSONObject("Attributes"));
		JSONArray recordings = jsonObject.getJSONArray("Recordings");

		for (int i = 0; i < recordings.length(); ++i) {
			this.recordings.add(new KVStreamRecordingData(recordings.getJSONObject(i)));
		}

	}

	public boolean hasRecordings() {
		return this.recordings.size() > 0;
	}

	public List<KVStreamRecordingData> getRecordings() {
		return this.recordings;
	}

	public String getAwsAccountId() {
		return this.awsAccountId;
	}

	public ContactFlowAttributes getAttributes() {
		return this.attributes;
	}

	public String getContactId() {
		return this.contactId;
	}

	public CustomerEndpoint getCustomerEndpoint() {
		return this.customerEndpoint;
	}
}
