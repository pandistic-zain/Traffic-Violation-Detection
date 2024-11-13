package com.Riphah.PDC.Traffic.Violation.Detection.Services;

import com.Riphah.PDC.Traffic.Violation.Detection.Entity.TrafficViolation;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
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
        for (int i = 0; i < framePaths.size(); i++) {
            String framePath = framePaths.get(i);
            TrafficViolation violation = sendFrameToFlaskService(framePath);
            if (violation != null) {
                violations.add(violation);
            } else {
                System.out.println("No violation detected for frame: " + framePath);
            }
        }
        return violations;
    }

    @SuppressWarnings("null")
    private TrafficViolation sendFrameToFlaskService(String framePath) {
        try {
            // Read the frame file into bytes
            byte[] frameBytes = Files.readAllBytes(Paths.get(framePath));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(frameBytes) {
                @Override
                public String getFilename() {
                    return new File(framePath).getName(); // Use actual filename
                }
            });

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            String flaskUrl = "https://e239-35-236-198-153.ngrok-free.app/analyze";
            System.out.println("Sending request to Flask at URL: " + flaskUrl);

            // Send request and receive image response
            ResponseEntity<byte[]> response = restTemplate.postForEntity(flaskUrl, requestEntity, byte[].class);

            if (response.getStatusCode().is2xxSuccessful()) {
                // Debugging response headers and content type
                System.out.println("Flask Response Status: " + response.getStatusCode());
                System.out.println("Response Content-Type: " + response.getHeaders().getContentType());
                System.out.println("Response Body Length: " + response.getBody().length);

                // Ensure the response body is not empty and contains valid image data
                if (response.getBody() != null && response.getBody().length > 0) {
                    String processedFrameDirectoryPath = "processed_frames";

                    File processedFrameDirectory = new File(processedFrameDirectoryPath);
                    if (!processedFrameDirectory.exists()) {
                        if (processedFrameDirectory.mkdirs()) {
                            System.out.println("Created processed frames directory:" + processedFrameDirectoryPath);
                        } else {
                            System.out.println("Failed to create directory: "+ processedFrameDirectoryPath);
                        }
                    }

                    // Define the path for the processed frame to be saved in the new directory
                    String processedFramePath = "processed_frames" + File.separator + new File(framePath).getName();

                    // Debugging where the processed file is being saved
                    System.out.println("Saving processed frame to: " + processedFramePath);

                    // Save the processed image to the new directory
                    Files.write(Paths.get(processedFramePath), response.getBody());
                    System.out.println("Processed frame saved: " + processedFramePath);

                    // Create a TrafficViolation instance with the processed frame data
                    return new TrafficViolation(
                            null,
                            response.getBody(), // Save processed video data here
                            LocalDateTime.now());
                } else {
                    System.out.println("Error: Received empty body from Flask for frame: " + framePath);
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