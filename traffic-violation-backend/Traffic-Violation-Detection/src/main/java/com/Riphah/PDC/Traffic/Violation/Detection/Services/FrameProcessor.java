package com.Riphah.PDC.Traffic.Violation.Detection.Services;

// import com.Riphah.PDC.Traffic.Violation.Detection.Entity.TrafficViolation;
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
// import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class FrameProcessor implements Callable<List<String>> {
    private final List<String> framePaths;
    private final RestTemplate restTemplate;

    public FrameProcessor(List<String> framePaths) {
        this.framePaths = framePaths;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public List<String> call() {
        List<String> processedFramePaths = new ArrayList<>();  // Store paths of processed frames

        // Send each frame to Flask for processing and save the processed image
        for (String framePath : framePaths) {
            boolean isProcessed = sendFrameToFlaskService(framePath, processedFramePaths);
            if (!isProcessed) {
                System.out.println("No valid response for frame: " + framePath);
            }
        }

        return processedFramePaths; // Return list of processed frame paths
    }

    @SuppressWarnings("null")
    private boolean sendFrameToFlaskService(String framePath, List<String> processedFramePaths) {
        try {
            byte[] frameBytes = Files.readAllBytes(Paths.get(framePath));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(frameBytes) {
                @Override
                public String getFilename() {
                    return new File(framePath).getName();
                }
            });

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            String flaskUrl = "https://23c7-34-125-72-221.ngrok-free.app/analyze";
            System.out.println("Sending request to Flask at URL: " + flaskUrl);

            ResponseEntity<byte[]> response = restTemplate.postForEntity(flaskUrl, requestEntity, byte[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().length > 0) {
                String projectDirectory = System.getProperty("user.dir");
                String processedFrameDirectoryPath = projectDirectory + File.separator + "processed_frames";

                File processedFrameDirectory = new File(processedFrameDirectoryPath);
                if (!processedFrameDirectory.exists() && processedFrameDirectory.mkdirs()) {
                    System.out.println("Created processed frames directory: " + processedFrameDirectoryPath);
                }

                String processedFramePath = processedFrameDirectoryPath + File.separator + new File(framePath).getName();
                Files.write(Paths.get(processedFramePath), response.getBody());
                System.out.println("Processed frame saved: " + processedFramePath);

                // Add the saved path to the list of processed frames
                processedFramePaths.add(processedFramePath);

                return true; // Indicate successful processing
            } else {
                System.out.println("Error: Received empty body from Flask for frame: " + framePath);
            }
        } catch (IOException e) {
            System.out.println("Error reading frame file: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Connection error with Flask: " + e.getMessage());
        }

        return false; // Indicate processing failure
    }
}
