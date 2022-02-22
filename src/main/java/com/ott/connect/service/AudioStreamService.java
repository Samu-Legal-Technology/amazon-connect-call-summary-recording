//package com.ott.connect.service;
//
//import com.amazonaws.auth.AWSCredentialsProvider;
//import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
//
//import com.amazonaws.regions.Regions;
//import com.connect.util.AudioUtils;
//import com.ott.connect.ContactVoicemailRepo;
//import com.ott.connect.S3UploadInfo;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.nio.ByteBuffer;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.Optional;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//public class AudioStreamService {
//	private static final Regions REGION = Regions.fromName(System.getenv("APP_REGION"));
//	private static final String RECORDINGS_BUCKET_NAME = System.getenv("RECORDINGS_BUCKET_NAME");
//	private static final String RECORDINGS_KEY_PREFIX = System.getenv("RECORDINGS_KEY_PREFIX");
//	private static final boolean RECORDINGS_PUBLIC_READ_ACL = Boolean
//			.parseBoolean(System.getenv("RECORDINGS_PUBLIC_READ_ACL"));
//	private static final String START_SELECTOR_TYPE = System.getenv("START_SELECTOR_TYPE");
//	private static final Logger logger = LoggerFactory.getLogger(AudioStreamService.class);
//	private TranscribeService transcribeService;
//	private ContactVoicemailRepo contactVoicemailRepo;
//
//	public AudioStreamService(TranscribeService transcribeService, ContactVoicemailRepo contactVoicemailRepo) {
//		this.transcribeService = transcribeService;
//		this.contactVoicemailRepo = contactVoicemailRepo;
//	}
//
//	public void processAudioStream(String streamARN, String startFragmentNum, String agentId, String agentName,
//			String contactId, boolean transcribeEnabled, boolean encryptionEnabled, Optional<String> languageCode,
//			Optional<Boolean> saveCallRecording) throws Exception {
//		logger.info(String.format(
//				"StreamARN=%s, startFragmentNum=%s, contactId=%stranscribeEnabled=%s, encryptionEnabled=%s", streamARN,
//				startFragmentNum, contactId, transcribeEnabled, encryptionEnabled));
//		long unixTime = System.currentTimeMillis() / 1000L;
//		Path saveAudioFilePath = Paths.get("/tmp", contactId + "_" + unixTime + ".raw");
//		System.out.println(
//				String.format("Save Path: %s Start Selector Type: %s", saveAudioFilePath, START_SELECTOR_TYPE));
//		FileOutputStream fileOutputStream = new FileOutputStream(saveAudioFilePath.toString());
//		String streamName = streamARN.substring(streamARN.indexOf("/") + 1, streamARN.lastIndexOf("/"));
//		InputStream kvsInputStream = KVSUtils.getInputStreamFromKVS(streamName, REGION, startFragmentNum,
//				getAWSCredentials(), START_SELECTOR_TYPE);
//		StreamingMkvReader streamingMkvReader = StreamingMkvReader
//				.createDefault(new InputStreamParserByteSource(kvsInputStream));
//		BasicMkvTagProcessor tagProcessor = new BasicMkvTagProcessor();
//		FragmentMetadataVisitor fragmentVisitor = FragmentMetadataVisitor.create(Optional.of(tagProcessor));
//
//		try {
//			logger.info("Saving audio bytes to location");
//
//			for (ByteBuffer audioBuffer = KVSUtils.getByteBufferFromStream(streamingMkvReader, fragmentVisitor,
//					tagProcessor, contactId); audioBuffer.remaining() > 0; audioBuffer = KVSUtils
//							.getByteBufferFromStream(streamingMkvReader, fragmentVisitor, tagProcessor, contactId)) {
//				byte[] audioBytes = new byte[audioBuffer.remaining()];
//				audioBuffer.get(audioBytes);
//				fileOutputStream.write(audioBytes);
//			}
//		} finally {
//			logger.info(
//					String.format("Closing file and upload raw audio for contactId: %s ... %s Save Call Recording: %b",
//							contactId, saveAudioFilePath, saveCallRecording));
//			this.closeFileAndUploadRawAudio(kvsInputStream, fileOutputStream, saveAudioFilePath, agentId, contactId,
//					unixTime, saveCallRecording, transcribeEnabled, encryptionEnabled, (String) languageCode.get());
//		}
//
//	}
//
//	private void closeFileAndUploadRawAudio(InputStream kvsInputStream, FileOutputStream fileOutputStream,
//			Path saveAudioFilePath, String agentId, String contactId, long unixTime,
//			Optional<Boolean> saveCallRecording, boolean transcribeEnabled, boolean encryptionEnabled,
//			String languageCode) throws IOException {
//		kvsInputStream.close();
//		fileOutputStream.close();
//		logger.info(String.format("Save call recording: %b", saveCallRecording));
//		logger.info(String.format("File size: %d", (new File(saveAudioFilePath.toString())).length()));
//		if ((Boolean) saveCallRecording.orElse(false) && (new File(saveAudioFilePath.toString())).length() > 0L) {
//			S3UploadInfo uploadInfo = AudioUtils.uploadRawAudio(REGION, RECORDINGS_BUCKET_NAME, RECORDINGS_KEY_PREFIX,
//					saveAudioFilePath.toString(), agentId, contactId, RECORDINGS_PUBLIC_READ_ACL, getAWSCredentials());
//			String transcriptJobName = contactId + "_" + unixTime;
//			if (transcribeEnabled) {
//				this.contactVoicemailRepo.createRecord(unixTime, agentId, true, "IN_PROGRESS", encryptionEnabled,
//						uploadInfo);
//				this.transcribeService.transcribeMediaUrl(uploadInfo.getResourceUrl(), transcriptJobName, languageCode);
//			} else {
//				this.contactVoicemailRepo.createRecord(unixTime, agentId, false, (String) null, encryptionEnabled,
//						uploadInfo);
//			}
//		} else {
//			logger.info("Skipping upload to S3.  saveCallRecording was disabled or audio file has 0 bytes: "
//					+ saveAudioFilePath);
//		}
//
//	}
//
//	private static AWSCredentialsProvider getAWSCredentials() {
//		return DefaultAWSCredentialsProviderChain.getInstance();
//	}
//}
