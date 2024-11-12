package com.Riphah.PDC.Traffic.Violation.Detection.Services;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.client.RestTemplate;

import com.Riphah.PDC.Traffic.Violation.Detection.Entity.TrafficViolation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class FrameProcessor implements Callable<List<TrafficViolation>> {
    private final List<String> framePaths;
    private final RestTemplate restTemplate;

    public FrameProcessor(List<String> framePaths) {
        this.framePaths = framePaths;
        this.restTemplate = new RestTemplate(); // Initialize RestTemplate
    }

    @Override
    public List<TrafficViolation> call() {
        List<TrafficViolation> violations = new ArrayList<>();
        for (String framePath : framePaths) {
            TrafficViolation violation = sendFrameToFlaskService(framePath);
            if (violation != null) {
                violations.add(violation);
            }
        }
        return violations;
    }

    public TrafficViolation sendFrameToFlaskService(String framePath) {
        try {
            byte[] frameBytes = Files.readAllBytes(new File(framePath).toPath());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(frameBytes) {
                @Override
                public String getFilename() {
                    return "frame.png";
                }
            });

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            String flaskUrl = "https://f64e-34-106-188-43.ngrok-free.app/analyze";
            System.out.println("Sending request to Flask at URL: " + flaskUrl);

            ResponseEntity<String> response = restTemplate.postForEntity(flaskUrl, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String responseBody = response.getBody();
                System.out.println("Received successful response from Flask for " + framePath + ": " + responseBody);

                if (responseBody != null && responseBody.contains("violation")) {
                    TrafficViolation violation = new TrafficViolation();
                    return violation;
                }
            } else {
                System.out.println("Error response from Flask: Status code - " + response.getStatusCode());
            }
        } catch (IOException e) {
            System.out.println("Error reading frame file: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Connection error with Flask: " + e.getMessage());
        }
        return null;
    }
}
