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
            directory.mkdirs(); // Create the directory if it does not exist
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

        // Create output directory if it does not exist
        File dir = new File(outputDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        List<String> framePaths = new ArrayList<>();
        while (videoCapture.read(frame)) {
            String frameFileName = String.format("%s/frame_%04d.png", outputDirectory, frameCount);
            Imgcodecs.imwrite(frameFileName, frame);
            framePaths.add(frameFileName);
            System.out.println("Extracted frame: " + frameFileName);
            frameCount++;
        }

        videoCapture.release();
        System.out.println("Frame extraction completed. Total frames extracted: " + frameCount);

        // Delete the original video file after frame extraction
        deleteFile(videoFilePath);

        // Process frames using multiple threads
        processFramesInParallel(framePaths);

        // // Delete extracted frames after processing
        // deleteExtractedFrames(framePaths);
    }

    

    private void processFramesInParallel(List<String> framePaths) {
        System.out.println("Starting parallel frame processing...");

        int availableCores = Runtime.getRuntime().availableProcessors();
        System.out.println("Available CPU cores: " + availableCores);

        int framesPerThread = (int) Math.ceil((double) framePaths.size() / availableCores);
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < availableCores; i++) {
            int start = i * framesPerThread;
            int end = Math.min(start + framesPerThread, framePaths.size());

            if (start < end) {
                List<String> framesSubset = framePaths.subList(start, end);
                System.out.println("Assigning frames " + start + " to " + (end - 1) + " to a new thread.");

                // Create and start a new thread for each subset of frames using FrameProcessor
                FrameProcessor frameProcessor = new FrameProcessor(framesSubset);
                Thread thread = new Thread(frameProcessor);
                thread.start();
                threads.add(thread);
            }
        }

        // Wait for all threads to complete using a simple for loop
        for (int i = 0; i < threads.size(); i++) {
            try {
                threads.get(i).join();
            } catch (InterruptedException e) {
                System.out.println("Thread interrupted: " + e.getMessage());
            }
        }

        System.out.println("All threads have completed processing.");
    }

    private class FrameProcessor implements Runnable {
        private final List<String> framePaths;

        public FrameProcessor(List<String> framePaths) {
            this.framePaths = framePaths;
        }

        @Override
        public void run() {
            for (int i = 0; i < framePaths.size(); i++) {
                String framePath = framePaths.get(i);
                // System.out.println("Sending frame " + framePath + " to Flask microservice for processing.");
                sendFrameToFlaskService(framePath);
            }
        }

        private void sendFrameToFlaskService(String framePath) {
            // System.out.println("Processing frame " + framePath + " at Flask microservice...");
            // Code for sending the frame to Flask service
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
