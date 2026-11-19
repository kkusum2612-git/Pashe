
package com.pashe.app.medicine.prescription;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/medicine/prescriptions")
public class MedicinePrescriptionController {
    private final MedicinePrescriptionService service;

    public MedicinePrescriptionController(MedicinePrescriptionService service) {
        this.service = service;
    }

    @GetMapping
    public List<MedicinePrescription> list(Principal principal) {
        return service.list(UUID.fromString(principal.getName()));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MedicinePrescription create(Principal principal, @RequestParam String doctorName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate prescriptionDate,
            @RequestParam(required = false) MultipartFile image) throws IOException {
        return service.create(UUID.fromString(principal.getName()), doctorName, prescriptionDate, image);
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> image(Principal principal, @PathVariable UUID id) {
        var image = service.getImage(UUID.fromString(principal.getName()), id);
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore().cachePrivate())
            .contentType(MediaType.parseMediaType(image.contentType()))
            .header("X-Content-Type-Options", "nosniff")
            .body(image.bytes());
    }
}
