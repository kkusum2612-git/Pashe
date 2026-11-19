package com.pashe.app.medicine.preferences;

import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/medicine/preferences")
public class MedicinePreferencesController {

    private final MedicinePreferencesService service;

    public MedicinePreferencesController(
            MedicinePreferencesService service) {
        this.service = service;
    }

    @GetMapping
    public MedicinePreferences getPreferences(
            Principal principal) {

        UUID userId = UUID.fromString(principal.getName());

        return service.getPreferences(userId);
    }

    @PutMapping
    public MedicinePreferences savePreferences(
            Principal principal,
            @RequestBody MedicinePreferences preferences) {

        UUID userId = UUID.fromString(principal.getName());

        return service.savePreferences(userId, preferences);
    }
}