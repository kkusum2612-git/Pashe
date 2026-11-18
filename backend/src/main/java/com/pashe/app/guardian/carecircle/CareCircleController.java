package com.pashe.app.guardian.carecircle;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
@RestController
@RequestMapping("/api/v1/guardian/care-circle")
public class CareCircleController {
    private final CareCircleService service;
    public CareCircleController(CareCircleService service) { this.service=service; }
    public record Invitation(@NotBlank @Email @Size(max=254) String email,@NotBlank String relationshipType,@AssertTrue boolean consent) {}
    @GetMapping public List<CareCircleService.View> list(Authentication a,@RequestParam(defaultValue="0") int page) { return service.list(UUID.fromString(a.getName()),page); }
    @PostMapping("/invitations") @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String,String> invite(Authentication a,@Valid @RequestBody Invitation body) { service.invite(UUID.fromString(a.getName()),body.email(),body.relationshipType()); return Map.of("message","If eligible, the invitation is available in the recipient's Care Circle."); }
    @PostMapping({"/invitations/{id}/{action}","/memberships/{id}/{action}"}) @ResponseStatus(HttpStatus.NO_CONTENT)
    public void change(Authentication a,@PathVariable UUID id,@PathVariable String action) { service.change(UUID.fromString(a.getName()),id,action); }
}
