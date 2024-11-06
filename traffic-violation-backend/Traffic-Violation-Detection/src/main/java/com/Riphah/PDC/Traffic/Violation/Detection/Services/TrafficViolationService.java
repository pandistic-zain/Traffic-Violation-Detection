package com.Riphah.PDC.Traffic.Violation.Detection.Services;

import com.Riphah.PDC.Traffic.Violation.Detection.Entity.TrafficViolation;
import com.Riphah.PDC.Traffic.Violation.Detection.Repository.TrafficViolationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Service
public class TrafficViolationService {

    @Autowired
    private TrafficViolationRepository repository;

    @Autowired
    private RestTemplate restTemplate;

    public List<TrafficViolation> getAllViolations() {
        return repository.findAll();
    }

    public TrafficViolation saveViolation(TrafficViolation violation) {
        return repository.save(violation);
    }

    public void saveVideoFile(MultipartFile file, String uploadDir) throws IOException {
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs(); // Create directory if it does not exist
        }

        File videoFile = new File(directory, file.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(videoFile)) {
            fos.write(file.getBytes());
        }

        System.out.println("Received video file: " + videoFile.getAbsolutePath());

        // Call the extractFrames method after saving the video
        String outputFramesDir = "directory/output/frames/"; // Define your output directory for frames
        extractFrames(videoFile.getAbsolutePath(), outputFramesDir);
    }

    public void extractFrames(String videoFilePath, String outputDirectory) {
        VideoCapture videoCapture = new VideoCapture(videoFilePath);

        if (!videoCapture.isOpened()) {
            System.out.println("Error: Cannot open video file.");
            return;
        }

        Mat frame = new Mat();
        int frameCount = 0;
        List<String> framePaths = new ArrayList<>();
        File dir = new File(outputDirectory);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        while (videoCapture.read(frame)) {
            String frameFileName = String.format("%s/frame_%04d.png", outputDirectory, frameCount);
            Imgcodecs.imwrite(frameFileName, frame);
            framePaths.add(frameFileName);
            System.out.println("Extracted frame: " + frameFileName);
            frameCount++;
        }

        videoCapture.release();
        System.out.println("Frame extraction completed. Total frames extracted: " + frameCount);

        deleteFile(videoFilePath); // Delete original video after frame extraction
        processFramesInParallel(framePaths); // Process frames in parallel
        deleteExtractedFrames(framePaths); // Delete frames after processing
    }

    private void processFramesInParallel(List<String> framePaths) {
        System.out.println("Starting parallel frame processing...");
        int availableCores = Runtime.getRuntime().availableProcessors();
        int framesPerThread = (int) Math.ceil((double) framePaths.size() / availableCores);

        ExecutorService executorService = Executors.newFixedThreadPool(availableCores);
        List<Future<List<TrafficViolation>>> futures = new ArrayList<>();

        for (int i = 0; i < availableCores; i++) {
            int start = i * framesPerThread;
            int end = Math.min(start + framesPerThread, framePaths.size());

            if (start < end) {
                List<String> framesSubset = framePaths.subList(start, end);
                System.out.println("Assigning frames " + start + " to " + (end - 1) + " to a new thread.");
                futures.add(executorService.submit(new FrameProcessor(framesSubset)));
            }
        }

        executorService.shutdown();

        // Collect results from all threads in sequence and store in MongoDB
        for (Future<List<TrafficViolation>> future : futures) {
            try {
                List<TrafficViolation> violations = future.get();
                for (TrafficViolation violation : violations) {
                    saveViolation(violation);
                    System.out.println("Stored violation in DB: " + violation);
                }
            } catch (InterruptedException | ExecutionException e) {
                System.out.println("Error in thread execution: " + e.getMessage());
            }
        }

        System.out.println("All threads have completed processing.");
    }

    private class FrameProcessor implements Callable<List<TrafficViolation>> {
        private final List<String> framePaths;

        public FrameProcessor(List<String> framePaths) {
            this.framePaths = framePaths;
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

        private TrafficViolation sendFrameToFlaskService(String framePath) {
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
                String flaskUrl = "http://localhost:5000/analyze"; // Flask endpoint
                ResponseEntity<String> response = restTemplate.postForEntity(flaskUrl, requestEntity, String.class);

                // Parse response and create TrafficViolation object
                String responseBody = response.getBody();
                System.out.println("Response from Flask for " + framePath + ": " + responseBody);

                if (responseBody != null && responseBody.contains("violation")) { // Assuming Flask response contains 'violation'
                    TrafficViolation violation = new TrafficViolation();
                    violation.setVehicleNumber(parseVehicleNumber(responseBody));
                    violation.setViolationTime(parseTimeStamp(responseBody));
                    violation.setDetails(responseBody);
                    return violation;
                }
            } catch (IOException e) {
                System.out.println("Error sending frame to Flask: " + e.getMessage());
            }
            return null;
        }

        private String parseVehicleNumber(String responseBody) {
            // Implement parsing logic based on Flask response
            return "VEHICLE123"; // Placeholder value
        }

        private String parseTimeStamp(String responseBody) {
            // Implement timestamp parsing logic based on Flask response
            return "TIMESTAMP"; // Placeholder value
        }
    }

    private void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists() && file.delete()) {
            System.out.println("Deleted file: " + filePath);
        } else {
            System.out.println("Failed to delete file: " + filePath);
        }
    }

    private void deleteExtractedFrames(List<String> framePaths) {
        System.out.println("Deleting extracted frames...");
        for (String framePath : framePaths) {
            deleteFile(framePath);
        }
    }
}
