package com.audio.transcribe;

import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/api/transcribe")
public class TranscriptionController {
    private final OpenAiAudioTranscriptionModel transcriptionModel;

    public TranscriptionController(@Value("${spring.ai.openai.api-key}") String apiKey) {
        OpenAiAudioApi openAiAudioApi = new OpenAiAudioApi(apiKey);
        this.transcriptionModel
                = new OpenAiAudioTranscriptionModel(openAiAudioApi);
    }

    @PostMapping
    public ResponseEntity<String> transcribeAudio (@RequestParam("file")MultipartFile file) throws IOException {
        File tempFile = File.createTempFile("audio",".wav");
    }

}
