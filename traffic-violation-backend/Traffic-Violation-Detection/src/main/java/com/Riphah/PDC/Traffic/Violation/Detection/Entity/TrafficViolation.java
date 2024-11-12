package com.Riphah.PDC.Traffic.Violation.Detection.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Document(collection = "violations")
@Data // Generates getters, setters, toString, equals, and hashCode methods
@NoArgsConstructor // Generates a no-args constructor
@AllArgsConstructor // Generates an all-args constructor
public class TrafficViolation {
    @Id
    private String id;                 // MongoDB document ID
    private String violationType;       // Type of violation (e.g., speeding, signal jump)
}
