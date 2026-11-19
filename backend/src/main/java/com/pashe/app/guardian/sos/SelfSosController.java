package com.pashe.app.guardian.sos;

import java.time.Instant;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/guardian")
public class SelfSosController {
    private final SelfSosService service;
    public SelfSosController(SelfSosService service) { this.service=service; }
    private UUID actor(Authentication a) { return UUID.fromString(a.getName()); }
    public record Start(@NotNull UUID idempotencyKey,@Size(max=300) String pickupAddress) {}
    public record Resolution(@NotBlank @Size(max=500) String reason) {}
    public record Location(@NotNull Double latitude,@NotNull Double longitude,@NotNull Double accuracy,@NotNull Instant observedAt) {
        SelfSosRepository.Point point(){return new SelfSosRepository.Point(latitude,longitude,accuracy,observedAt);}
        @Override public String toString(){return "Location[redacted]";}
    }
    @PostMapping("/sos/requests") public SelfSosService.RequestView start(Authentication a,@Valid @RequestBody Start body){return service.start(actor(a),body.idempotencyKey(),body.pickupAddress());}
    @GetMapping("/sos/requests") public List<SelfSosService.RequestView> current(Authentication a){return service.current(actor(a));}
    @GetMapping("/sos/requests/{id}") public SelfSosService.RequestView request(Authentication a,@PathVariable UUID id){return service.request(actor(a),id);}
    @PostMapping("/sos/requests/{id}/cancel") @ResponseStatus(HttpStatus.NO_CONTENT) public void cancel(Authentication a,@PathVariable UUID id){service.cancel(actor(a),id);}
    @GetMapping("/incidents") public List<SelfSosService.IncidentView> list(Authentication a,@RequestParam(defaultValue="0") int page){return service.list(actor(a),page);}
    @GetMapping("/incidents/{id}") public SelfSosService.IncidentView view(Authentication a,@PathVariable UUID id){return service.view(actor(a),id);}
    @PostMapping("/incidents/{id}/resolve") @ResponseStatus(HttpStatus.NO_CONTENT) public void resolve(Authentication a,@PathVariable UUID id,@Valid @RequestBody Resolution body){service.resolve(actor(a),id,body.reason());}
    @PostMapping("/location") @ResponseStatus(HttpStatus.NO_CONTENT) public void location(Authentication a,@Valid @RequestBody Location body){service.location(actor(a),body.point());}
    @DeleteMapping("/location") @ResponseStatus(HttpStatus.NO_CONTENT) public void clear(Authentication a){service.clearLocation(actor(a));}
}
