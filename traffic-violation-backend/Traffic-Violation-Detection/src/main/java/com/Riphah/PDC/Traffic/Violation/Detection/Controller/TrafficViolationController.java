package com.Riphah.PDC.Traffic.Violation.Detection.Controller;

import java.io.IOException;
import com.Riphah.PDC.Traffic.Violation.Detection.Entity.TrafficViolation;
import com.Riphah.PDC.Traffic.Violation.Detection.Services.TrafficViolationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/violations")
public class TrafficViolationController {

    @Autowired
    private TrafficViolationService service;

    @GetMapping
    public List<TrafficViolation> getViolations() {
        return service.getAllViolations();
    }

    @PostMapping
    public TrafficViolation addViolation(@RequestBody TrafficViolation violation) {
        return service.saveViolation(violation);
    }
    @PostMapping("/upload")
public ResponseEntity<String> uploadVideo(@RequestParam("file") MultipartFile file) {
    try {
        String uploadDir = "Z:\\Traffic-Violation-Detection\\directory";
         service.saveVideoFile(file, uploadDir);

        return ResponseEntity.ok("Video uploaded successfully for processing.");
    } catch (IOException e) {
        e.printStackTrace();
        return ResponseEntity.status(500).body("Failed to process video file.");
    }
    }
}
