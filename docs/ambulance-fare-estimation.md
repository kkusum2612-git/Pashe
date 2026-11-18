# Ambulance Fare Estimation (FR-07)

## Overview

Calculates an upfront ambulance fare using actual road
distance obtained from OpenRouteService.

## Configuration

Obtain a Standard API key from https://account.heigit.org/.

Set HEIGIT_API_KEY in the backend environment before
starting Spring Boot. Never commit actual API keys.

Optional pricing environment variables:

- PASHE_AMBULANCE_BASE_FARE: 500.00 BDT
- PASHE_AMBULANCE_RATE_PER_KM: 40.00 BDT

These are demonstration rates.

## API endpoints

- GET /api/v1/ambulance/fare/locations?query=Dhanmondi
- POST /api/v1/ambulance/fare/estimate

Both endpoints require authentication.

## Fare formula

Total fare = Base fare + (Road distance × Rate per km)

## Architecture

- Spring Boot MVC backend
- Separate ambulance/fare package
- React fare estimator integrated into the ambulance directory
- External routing and geocoding through HeiGIT
- No database schema changes

## Testing

Run from the backend directory:

```powershell
.\mvnw.cmd "-Dtest=FareServiceTest" test
```

The frontend supports English and Bangla.