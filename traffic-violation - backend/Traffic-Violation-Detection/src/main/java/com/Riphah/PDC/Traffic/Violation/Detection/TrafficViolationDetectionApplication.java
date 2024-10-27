package com.Riphah.PDC.Traffic.Violation.Detection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TrafficViolationDetectionApplication {

	public static void main(String[] args) {
		SpringApplication.run(TrafficViolationDetectionApplication.class, args);
		System.out.println("-------------------------------------------------");
		System.out.println("|Trafic-Violation Dectector Running|");
		System.out.println("-------------------------------------------------");
	}

}
