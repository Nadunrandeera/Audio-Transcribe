package com.audio.transcribe.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class TranscriptionService {

    private static final String UPLOAD_URL = "https://api.assemblyai.com/v2/upload";
    private static final String TRANSCRIBE_URL = "https://api.assemblyai.com/v2/transcript";
    @Value("${assemblyai.api.key}")
    private String API_KEY;

    private final RestTemplate restTemplate;

    public TranscriptionService(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    public String transcribeAudio(MultipartFile audioFile) throws IOException, InterruptedException {
        // 1. Upload audio file
        HttpHeaders headers = new HttpHeaders();
        headers.set("authorization", API_KEY);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        HttpEntity<byte[]> uploadRequest = new HttpEntity<>(audioFile.getBytes(), headers);
        ResponseEntity<Map> uploadResponse = restTemplate.postForEntity(UPLOAD_URL, uploadRequest, Map.class);

        String uploadUrl = uploadResponse.getBody().get("upload_url").toString();

        // 2. Send for transcription
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = new HashMap<>();
        body.put("audio_url", uploadUrl);
        HttpEntity<Map<String, String>> transcriptionRequest = new HttpEntity<>(body, headers);

        ResponseEntity<Map> transcriptionResponse = restTemplate.postForEntity(TRANSCRIBE_URL, transcriptionRequest, Map.class);
        String transcriptId = transcriptionResponse.getBody().get("id").toString();

        // 3. Poll until complete (basic approach)
        int maxRetries = 20;
        int attempts = 0;
        String status = "";
        String text = "";
        try {
            while (true) {
                HttpEntity<Void> pollRequest = new HttpEntity<>(headers);
                ResponseEntity<Map> pollResponse = restTemplate.exchange(TRANSCRIBE_URL + "/" + transcriptId,
                        HttpMethod.GET,
                        pollRequest,
                        Map.class);

                if (pollResponse.getStatusCode() != HttpStatus.OK || pollResponse.getBody() == null) {
                    throw new RuntimeException("Failed to poll transcription status or received empty response.");
                }

                status = pollResponse.getBody().get("status").toString();

                if (status.equals("completed")) {
                    text = pollResponse.getBody().get("text").toString();
                    break;
                } else if (status.equals("failed")) {
                    throw new RuntimeException("Transcription failed.");
                }

                Thread.sleep(3000); // wait 3 seconds before polling again
            }
            if (!status.equals("completed")) {
                throw new RuntimeException("Transcription polling timed out after " + maxRetries + " attempts (~1 minute).");
            }

        }catch (Exception e){
            throw new RuntimeException("Error during polling: " + e.getMessage(), e);
        }
        return text;
    }
}
