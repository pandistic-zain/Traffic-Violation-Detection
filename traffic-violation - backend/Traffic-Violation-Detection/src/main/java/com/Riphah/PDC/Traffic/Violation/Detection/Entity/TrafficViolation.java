package com.Riphah.PDC.Traffic.Violation.Detection.Entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Document(collection = "violations")
public class TrafficViolation {
    @Id
    private String id;
    private String vehicleNumber;
    private String violationType;
    private Date timestamp;

    // Getters and Setters
}
