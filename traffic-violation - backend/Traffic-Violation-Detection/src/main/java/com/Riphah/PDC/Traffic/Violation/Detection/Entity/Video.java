
package com.Riphah.PDC.Traffic.Violation.Detection.Entity;

import lombok.Data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "videos")
public class Video {
    @Id
    private String id;
    private String filename;
    private String status;
}