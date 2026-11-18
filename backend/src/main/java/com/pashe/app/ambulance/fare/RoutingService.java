package com.pashe.app.ambulance.fare;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pashe.app.ambulance.fare.FareDtos.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class RoutingService {

    private final RestClient client;
    private final String apiKey;

    public RoutingService(
            @Value("${HEIGIT_API_KEY:}") String apiKey
    ) {
        this.apiKey = apiKey;

        var factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .build()
        );

        factory.setReadTimeout(Duration.ofSeconds(15));

        this.client = RestClient.builder()
                .baseUrl("https://api.heigit.org")
                .requestFactory(factory)
                .build();
    }

    // Search for locations in Bangladesh
    public List<LocationResponse> searchLocations(String query) {

        checkApiKey();

        if (query == null || query.trim().length() < 3
                || query.length() > 150) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Enter a location between 3 and 150 characters"
            );
        }

        try {
            GeocodeResponse response = client.get()
                    .uri(builder -> builder
                            .path("/pelias/v1/search")
                            .queryParam("text", "{query}")
                            .queryParam("boundary.country", "BD")
                            .queryParam("size", 5)
                            .build(query.trim()))
                    .header("Authorization", apiKey)
                    .retrieve()
                    .body(GeocodeResponse.class);

            if (response == null || response.features() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Invalid location search response"
                );
            }

            return response.features().stream()
                    .map(this::toLocation)
                    .filter(Objects::nonNull)
                    .toList();

        } catch (RestClientException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Location search is currently unavailable"
            );
        }
    }

    // Calculate road distance in meters
    public BigDecimal getDistanceMeters(
            Coordinates pickup,
            Coordinates destination
    ) {
        checkApiKey();

        // OpenRouteService expects longitude before latitude
        var body = Map.of(
                "coordinates", List.of(
                        List.of(
                                pickup.longitude(),
                                pickup.latitude()
                        ),
                        List.of(
                                destination.longitude(),
                                destination.latitude()
                        )
                ),
                "instructions", false,
                "units", "m"
        );

        try {
            DirectionsResponse response = client.post()
                    .uri("/openrouteservice/v2/directions/driving-car")
                    .header("Authorization", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(DirectionsResponse.class);

            if (response == null
                    || response.routes() == null
                    || response.routes().isEmpty()
                    || response.routes().getFirst().summary() == null
                    || response.routes().getFirst().summary().distance() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Unable to determine road distance"
                );
            }

            BigDecimal distance = response.routes()
                    .getFirst()
                    .summary()
                    .distance();

            if (distance.signum() < 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Invalid road distance"
                );
            }

            return distance;

        } catch (RestClientException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to calculate a driving route"
            );
        }
    }

    private LocationResponse toLocation(Feature feature) {

        if (feature == null
                || feature.geometry() == null
                || feature.properties() == null
                || feature.properties().label() == null
                || feature.geometry().coordinates() == null
                || feature.geometry().coordinates().size() < 2) {
            return null;
        }

        var coordinates = feature.geometry().coordinates();

        if (coordinates.get(0) == null || coordinates.get(1) == null) {
            return null;
        }

        return new LocationResponse(
                feature.properties().label(),
                coordinates.get(1),
                coordinates.get(0)
        );
    }

    private void checkApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Routing service is not configured"
            );
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeocodeResponse(List<Feature> features) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Feature(Geometry geometry, Properties properties) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Geometry(List<BigDecimal> coordinates) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Properties(String label) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DirectionsResponse(List<Route> routes) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Route(Summary summary) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Summary(BigDecimal distance) {}
}