
package com.pashe.app.medicine.safety;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/medicine/safety")
class MedicineSafetyController {
    private final MedicineSafetyService service;

    MedicineSafetyController(MedicineSafetyService service) {
        this.service = service;
    }

    @GetMapping
    MedicineSafetyReport list(Principal principal) {
        return service.list(UUID.fromString(principal.getName()));
    }

    @PutMapping("/{warningId}/acknowledgement")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void acknowledge(Principal principal, @PathVariable String warningId, @RequestBody Acknowledgement request) {
        service.acknowledge(UUID.fromString(principal.getName()), warningId, request.intentional());
    }

    record Acknowledgement(boolean intentional) {}
}
