package com.Riphah.PDC.Traffic.Violation.Detection.Controller;

import com.Riphah.PDC.Traffic.Violation.Detection.Entity.TrafficViolation;
import com.Riphah.PDC.Traffic.Violation.Detection.Services.TrafficViolationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
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
}
