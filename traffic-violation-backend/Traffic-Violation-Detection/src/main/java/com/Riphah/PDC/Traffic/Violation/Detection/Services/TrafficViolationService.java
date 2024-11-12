package com.Riphah.PDC.Traffic.Violation.Detection.Services;

import com.Riphah.PDC.Traffic.Violation.Detection.Entity.TrafficViolation;
import com.Riphah.PDC.Traffic.Violation.Detection.Repository.TrafficViolationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Service
public class TrafficViolationService {

    @Autowired
    private TrafficViolationRepository repository;

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

public void processFramesInParallel(List<String> framePaths) {
    System.out.println("Starting parallel frame processing...");
    
    int availableCores = Runtime.getRuntime().availableProcessors();
    int framesPerThread = (int) Math.ceil((double) framePaths.size() / availableCores);
    
    ExecutorService executorService = Executors.newFixedThreadPool(availableCores); // Use ExecutorService
    List<Future<List<TrafficViolation>>> futures = new ArrayList<>();

    // Submit tasks to process frames in parallel
    for (int i = 0; i < availableCores; i++) {
        int start = i * framesPerThread;
        int end = Math.min(start + framesPerThread, framePaths.size());

        if (start < end) {
            List<String> framesSubset = framePaths.subList(start, end);
            System.out.println("Assigning frames " + start + " to " + (end - 1) + " to a new task.");

            // Submit FrameProcessor task to executor service
            Future<List<TrafficViolation>> future = executorService.submit(new FrameProcessor(framesSubset));
            futures.add(future);
        }
    }

    // Thread Synchronization with join(): If you were using Thread directly (not ExecutorService), you would invoke join() on each thread to ensure that the main thread waits for each worker thread to complete. However, with ExecutorService and Future, join() is not necessary because get() on a Future already blocks until the task is complete.
    for (int i = 0; i < futures.size(); i++) {
        try {
            List<TrafficViolation> violations = futures.get(i).get(); // Retrieve the result
            if (violations != null && !violations.isEmpty()) {
                for (int j = 0; j < violations.size(); j++) {
                    saveViolation(violations.get(j));  // Save each violation
                }
                System.out.println("Stored violation in DB");
            }
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("Error processing violation results: " + e.getMessage());
        }
    }

    // Shutdown the executor
    executorService.shutdown();
    System.out.println("All threads have completed processing.");
}

    
    

    private void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists() && file.delete()) {
        } else {
            System.out.println("Failed to delete file: " + filePath);
        }
    }

    private void deleteExtractedFrames(List<String> framePaths) {
        
        for (String framePath : framePaths) {
            deleteFile(framePath);
        }
        System.out.println("Deleted extracted frames...");
    }
}
