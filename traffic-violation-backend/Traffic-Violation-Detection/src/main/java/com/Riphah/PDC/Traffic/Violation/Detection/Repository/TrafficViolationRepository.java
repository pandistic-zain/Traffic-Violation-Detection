package com.Riphah.PDC.Traffic.Violation.Detection.Repository;

import com.Riphah.PDC.Traffic.Violation.Detection.Entity.TrafficViolation;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TrafficViolationRepository extends MongoRepository<TrafficViolation, String> {
}
