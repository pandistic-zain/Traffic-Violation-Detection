package com.Riphah.PDC.Traffic.Violation.Detection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories(basePackages = "com.Riphah.PDC.Traffic.Violation.Detection.Repository")
public class TrafficViolationDetectionApplication {

	public static void main(String[] args) {
		SpringApplication.run(TrafficViolationDetectionApplication.class, args);
		System.out.println("-------------------------------------------------");
		System.out.println("|Trafic-Violation Dectector Running|");
		System.out.println("-------------------------------------------------");
	}

}
