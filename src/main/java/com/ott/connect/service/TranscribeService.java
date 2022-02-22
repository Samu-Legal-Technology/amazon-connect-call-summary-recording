package com.ott.connect.service;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.transcribe.AmazonTranscribe;
import com.amazonaws.services.transcribe.AmazonTranscribeClient;
import com.amazonaws.services.transcribe.AmazonTranscribeClientBuilder;
import com.amazonaws.services.transcribe.model.GetTranscriptionJobRequest;
import com.amazonaws.services.transcribe.model.GetTranscriptionJobResult;
import com.amazonaws.services.transcribe.model.LanguageCode;
import com.amazonaws.services.transcribe.model.Media;
import com.amazonaws.services.transcribe.model.StartTranscriptionJobRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class TranscribeService {
	private AmazonTranscribe transcribe;

	public TranscribeService(Regions region) {
		this.transcribe = (AmazonTranscribe) ((AmazonTranscribeClientBuilder) AmazonTranscribeClient.builder()
				.withRegion(region)).build();
	}

	public void transcribeMediaUrl(String url, String jobName, String languageCode) {
		StartTranscriptionJobRequest request = new StartTranscriptionJobRequest();
		request.withLanguageCode(LanguageCode.fromValue(languageCode));
		Media media = new Media();
		media.setMediaFileUri(url);
		request.withMedia(media).withMediaSampleRateHertz(8000);
		request.setTranscriptionJobName(jobName);
		request.withMediaFormat("wav");
		this.transcribe.startTranscriptionJob(request);
	}

	public void getTranscript(String jobName) {
		GetTranscriptionJobRequest request = new GetTranscriptionJobRequest();
		request.setTranscriptionJobName(jobName);
		GetTranscriptionJobResult result = this.transcribe.getTranscriptionJob(request);

		try {
			String transcriptResult = this
					.downloadTranscript(result.getTranscriptionJob().getTranscript().getTranscriptFileUri());
			JSONObject json = new JSONObject(transcriptResult);
			String var6 = json.getJSONObject("results").getJSONArray("transcripts").getJSONObject(0)
					.getString("transcript");
		} catch (Exception var7) {
			System.out.println("Error getting the transcript");
		}

	}

	private String downloadTranscript(String uri) throws IOException {
		StringBuilder result = new StringBuilder();
		URL url = new URL(uri);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));

		String line;
		while ((line = rd.readLine()) != null) {
			result.append(line);
		}

		rd.close();
		return result.toString();
	}
}
