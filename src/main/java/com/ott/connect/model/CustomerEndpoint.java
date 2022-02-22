package com.ott.connect.model;

import org.json.JSONObject;

public class CustomerEndpoint {
	private String address;
	private String type;

	public CustomerEndpoint(JSONObject jsonObject) {
		this.address = jsonObject.getString("Address");
		this.type = jsonObject.getString("Type");
	}

	public String getAddress() {
		return this.address;
	}

	public String getType() {
		return this.type;
	}
}