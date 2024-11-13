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
        deleteExtractedFrames(framePaths); // Delete frames after processing
    }

    public void processFramesInParallel(List<String> framePaths) {
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
                System.out.println("Assigning frames " + start + " to " + (end - 1) + " to a new task.");

                Future<List<TrafficViolation>> future = executorService.submit(new FrameProcessor(framesSubset));
                futures.add(future);
            }
        }

        List<String> processedFramePaths = new ArrayList<>();
        for (Future<List<TrafficViolation>> future : futures) {
            try {
                List<TrafficViolation> violations = future.get();
                violations.forEach(violation -> repository.save(violation));
            } catch (InterruptedException | ExecutionException e) {
                System.out.println("Error processing frame results: " + e.getMessage());
            }
        }

        reconstructVideoFromFrames(processedFramePaths, "output/video_output.mp4");
        executorService.shutdown();
        System.out.println("All threads have completed processing.");
    }

    public void reconstructVideoFromFrames(List<String> framePaths, String outputVideoPath) {
        if (framePaths.isEmpty()) {
            System.out.println("No frames to reconstruct.");
            return;
        }

        framePaths.sort(Comparator.naturalOrder());
        Mat firstFrame = Imgcodecs.imread(framePaths.get(0));
        int width = firstFrame.width();
        int height = firstFrame.height();
        Size frameSize = new Size(width, height);

        VideoWriter videoWriter = new VideoWriter(outputVideoPath, VideoWriter.fourcc('M', 'J', 'P', 'G'), 30, frameSize);

        if (!videoWriter.isOpened()) {
            System.out.println("Error: Cannot open video writer.");
            return;
        }

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

        // Save reconstructed video to database
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

    private void deleteExtractedFrames(List<String> framePaths) {
        for (String framePath : framePaths) {
            deleteFile(framePath);
        }
        System.out.println("Deleted extracted frames...");
    }
}
