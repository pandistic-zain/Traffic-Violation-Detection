package com.Riphah.PDC.Traffic.Violation.Detection.Repository;

import com.Riphah.PDC.Traffic.Violation.Detection.Entity.Video;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface VideoRepository extends MongoRepository<Video, String> {
    // Additional query methods can be defined here if needed
}
