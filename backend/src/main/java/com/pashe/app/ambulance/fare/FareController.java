package com.pashe.app.ambulance.fare;

import com.pashe.app.ambulance.fare.FareDtos.*;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ambulance/fare")
public class FareController {

    private final FareService fareService;
    private final RoutingService routingService;

    public FareController(
            FareService fareService,
            RoutingService routingService
    ) {
        this.fareService = fareService;
        this.routingService = routingService;
    }

    @GetMapping("/locations")
    public List<LocationResponse> searchLocations(
            @RequestParam("query") String query
    ) {
        return routingService.searchLocations(query);
    }

    @PostMapping("/estimate")
    public FareResponse estimateFare(
            @Valid @RequestBody FareEstimateRequest request
    ) {
        return fareService.estimateFare(request);
    }
}