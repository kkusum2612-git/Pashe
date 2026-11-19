package com.pashe.app.medicine.medication;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/medicine/my-medicines")
public class UserMedicineController {

    private final UserMedicineService service;

    public UserMedicineController(UserMedicineService service) {
        this.service = service;
    }

    @GetMapping
    public List<UserMedicine> getMyMedicines(
            Principal principal) {

        UUID userId = UUID.fromString(principal.getName());

        return service.getMyMedicines(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserMedicine addMedicine(
            Principal principal,
            @Valid @RequestBody UserMedicineRequest request) {

        UUID userId = UUID.fromString(principal.getName());

        return service.addMedicine(userId, request);
    }

    @PostMapping("/preview")
    public MedicinePreview preview(Principal principal, @Valid @RequestBody UserMedicineRequest request) {
        return service.preview(UUID.fromString(principal.getName()), null, request);
    }

    @PostMapping("/{id}/preview")
    public MedicinePreview previewReplacement(Principal principal, @PathVariable UUID id,
            @Valid @RequestBody UserMedicineRequest request) {
        return service.preview(UUID.fromString(principal.getName()), id, request);
    }

    @PostMapping("/{id}/replacement")
    @ResponseStatus(HttpStatus.CREATED)
    public UserMedicine replace(Principal principal, @PathVariable UUID id,
            @Valid @RequestBody UserMedicineRequest request) {
        return service.replaceMedicine(UUID.fromString(principal.getName()), id, request);
    }

    @PatchMapping("/{id}/stop")
    public UserMedicine stopMedicine(
            Principal principal,
            @PathVariable UUID id,
            @Valid @RequestBody StopRequest request) {

        UUID userId = UUID.fromString(principal.getName());

        return service.stopMedicine(
            userId,
            id,
            request.reason()
        );
    }

    public record StopRequest(
        @NotBlank
        @Size(max = 255)
        String reason
    ) {}
}