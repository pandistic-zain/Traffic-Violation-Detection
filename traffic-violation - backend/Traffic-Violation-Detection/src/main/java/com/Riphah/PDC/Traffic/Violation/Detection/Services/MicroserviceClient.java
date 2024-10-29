package com.Riphah.PDC.Traffic.Violation.Detection.Services;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.io.ByteArrayResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Service
public class MicroserviceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    public String sendFrameToPythonMicroservice(File frame) throws IOException {
        // Convert the frame into a byte array
        byte[] frameBytes = Files.readAllBytes(frame.toPath());
        ByteArrayResource byteArrayResource = new ByteArrayResource(frameBytes) {
            @Override
            public String getFilename() {
                return "frame.jpg";  // Name required for multipart data
            }
        };

        // Create HTTP headers for multipart request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Prepare request with the image as a part
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("frame", byteArrayResource);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // Send the POST request to Python microservice
        String url = "http://localhost:5000/detect";
        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();  // Successful response
        } else {
            throw new RuntimeException("Failed to detect vehicles. Status code: " + response.getStatusCode());
        }
    }
}

