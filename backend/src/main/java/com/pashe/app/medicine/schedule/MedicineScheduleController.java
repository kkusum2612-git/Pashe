package com.pashe.app.medicine.schedule;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/medicine/schedule")
public class MedicineScheduleController {

    private final MedicineScheduleService service;

    public MedicineScheduleController(
            MedicineScheduleService service) {

        this.service = service;
    }

    @GetMapping
    public List<ScheduledDose> getSchedule(
            Principal principal,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        UUID userId = UUID.fromString(principal.getName());

        return service.getSchedule(userId, date);
    }
}