package com.Riphah.PDC.Traffic.Violation.Detection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories(basePackages = "com.Riphah.PDC.Traffic.Violation.Detection.Repository")
public class TrafficViolationDetectionApplication {

	public static void main(String[] args) {
		try {
			// Load the OpenCV library
			System.load("C:\\opencv\\build\\java\\x64\\opencv_java490.dll");
		} catch (UnsatisfiedLinkError e) {
			System.err.println("Failed to load OpenCV library: " + e.getMessage());
			System.exit(1);
		}

		SpringApplication.run(TrafficViolationDetectionApplication.class, args);
		System.out.println("-------------------------------------------------");
		System.out.println("| Traffic-Violation Detector Running |");
		System.out.println("-------------------------------------------------");
	}
}
