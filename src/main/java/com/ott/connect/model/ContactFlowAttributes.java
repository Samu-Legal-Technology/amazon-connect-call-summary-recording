package com.ott.connect.model;

import java.util.Optional;
import org.json.JSONObject;

public class ContactFlowAttributes {
	private String agentId;
	private String agentName;
	private String extensionNumber;
	private String transferMessage;
	private boolean encryptVoicemail;
	private Optional<Boolean> saveCallRecording;
	private boolean transcribeVoicemail;
	private Optional<String> languageCode;

	public ContactFlowAttributes(JSONObject jsonObject) {
		this.agentId = jsonObject.getString("agentId");
		this.agentName = jsonObject.getString("agentName");
		this.extensionNumber = jsonObject.getString("extensionNumber");
		this.transferMessage = jsonObject.getString("transferMessage");
		this.encryptVoicemail = Boolean.parseBoolean(jsonObject.getString("encryptVoicemail"));
		this.saveCallRecording = Optional.of(Boolean.parseBoolean(jsonObject.getString("saveCallRecording")));
		this.transcribeVoicemail = Boolean.parseBoolean(jsonObject.getString("transcribeVoicemail"));
		if (jsonObject.has("languageCode")) {
			this.languageCode = Optional.of(jsonObject.getString("languageCode"));
		} else {
			this.languageCode = Optional.of("en-US");
		}

	}

	public String getAgentId() {
		return this.agentId;
	}

	public String getAgentName() {
		return this.agentName;
	}

	public String getExtensionNumber() {
		return this.extensionNumber;
	}

	public String getTransferMessage() {
		return this.transferMessage;
	}

	public boolean isEncryptVoicemail() {
		return this.encryptVoicemail;
	}

	public Optional<Boolean> getSaveCallRecording() {
		return this.saveCallRecording;
	}

	public boolean isSaveCallRecording() {
		return (Boolean) this.saveCallRecording.orElse(false);
	}

	public boolean isTranscribeVoicemail() {
		return this.transcribeVoicemail;
	}

	public Optional<String> getLanguageCode() {
		return this.languageCode;
	}
}