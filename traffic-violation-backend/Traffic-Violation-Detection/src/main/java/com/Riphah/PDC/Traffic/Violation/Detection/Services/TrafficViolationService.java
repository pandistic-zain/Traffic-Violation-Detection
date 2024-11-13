package com.Riphah.PDC.Traffic.Violation.Detection.Services;

import com.Riphah.PDC.Traffic.Violation.Detection.Entity.TrafficViolation;
import com.Riphah.PDC.Traffic.Violation.Detection.Repository.TrafficViolationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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
    }

    public void processFramesInParallel(List<String> framePaths) {
        System.out.println("Starting parallel frame processing...");

        int availableCores = Runtime.getRuntime().availableProcessors();
        int framesPerThread = (int) Math.ceil((double) framePaths.size() / availableCores);

        ExecutorService executorService = Executors.newFixedThreadPool(availableCores);
        List<Future<List<String>>> futures = new ArrayList<>();

        for (int i = 0; i < availableCores; i++) {
            int start = i * framesPerThread;
            int end = Math.min(start + framesPerThread, framePaths.size());

            if (start < end) {
                List<String> framesSubset = framePaths.subList(start, end);
                System.out.println("Assigning frames " + start + " to " + (end - 1) + " to a new task.");

                Future<List<String>> future = executorService.submit(new FrameProcessor(framesSubset));
                futures.add(future);
            }
        }

        List<String> allProcessedFramePaths = new ArrayList<>();
        for (Future<List<String>> future : futures) {
            try {
                List<String> processedPaths = future.get();
                allProcessedFramePaths.addAll(processedPaths);
            } catch (InterruptedException | ExecutionException e) {
                System.out.println("Error processing frame results: " + e.getMessage());
            }
        }

        reconstructVideoFromFrames(allProcessedFramePaths, "output/video_output.mp4");
        executorService.shutdown();
        System.out.println("All threads have completed processing.");
    }

    public void reconstructVideoFromFrames(List<String> framePaths, String outputVideoPath) {
        if (framePaths.isEmpty()) {
            System.out.println("No frames to reconstruct.");
            return;
        }
    
        // Sort the frame paths to maintain the correct order
        framePaths.sort(Comparator.naturalOrder());
        System.out.println("Processed frames are sorted successfully...");
    
        // Read the first frame to get the video dimensions
        Mat firstFrame = Imgcodecs.imread(framePaths.get(0));
        if (firstFrame.empty()) {
            System.out.println("Error: First frame is empty. Cannot determine frame size.");
            return;
        }
        int width = firstFrame.width();
        int height = firstFrame.height();
        Size frameSize = new Size(width, height);
    
        // Ensure the output directory exists
        File outputFile = new File(outputVideoPath);
        File outputDir = outputFile.getParentFile();
        if (outputDir != null && !outputDir.exists()) {
            if (outputDir.mkdirs()) {
                System.out.println("Created output directory: " + outputDir.getAbsolutePath());
            } else {
                System.out.println("Error: Failed to create output directory.");
                return;
            }
        }
    
        // Create a VideoWriter object with a compatible codec
        VideoWriter videoWriter = new VideoWriter(outputVideoPath, VideoWriter.fourcc('X', 'V', 'I', 'D'), 30, frameSize);
    
        // Check if VideoWriter successfully opened
        if (!videoWriter.isOpened()) {
            System.out.println("Error: Cannot open video writer. Please check codec and output path.");
            return;
        }
    
        // Write each processed frame to the video
        for (String framePath : framePaths) {
            Mat frame = Imgcodecs.imread(framePath);
            if (frame.empty()) {
                System.out.println("Warning: Skipping empty frame at " + framePath);
                continue;
            }
            videoWriter.write(frame);
        }
    
        videoWriter.release();
        System.out.println("Video reconstruction completed: " + outputVideoPath);
    
        // Save reconstructed video to the database
        saveReconstructedVideoToDatabase(outputVideoPath);
    }
    
    private void saveReconstructedVideoToDatabase(String videoPath) {
        try {
            // Read video file into byte array
            byte[] videoData = Files.readAllBytes(Path.of(videoPath));

            // Create a new TrafficViolation entity with the video data
            TrafficViolation violation = new TrafficViolation(
                null,                     // ID (auto-generated)
                videoData,                // Processed video data
                LocalDateTime.now()       // Timestamp
            );

            // Save the violation entry to the database
            repository.save(violation);
            System.out.println("Reconstructed video saved to the database.");

        } catch (IOException e) {
            System.out.println("Error reading reconstructed video file: " + e.getMessage());
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
}
