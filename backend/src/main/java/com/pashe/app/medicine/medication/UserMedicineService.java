package com.pashe.app.medicine.medication;

import com.pashe.app.medicine.catalog.MedicineCatalogItem.Ingredient;
import com.pashe.app.medicine.catalog.MedicineCatalogService;
import com.pashe.app.medicine.preferences.MedicinePreferences;
import com.pashe.app.medicine.preferences.MedicinePreferencesService;
import com.pashe.app.medicine.prescription.MedicinePrescriptionService;
import com.pashe.app.medicine.schedule.MedicineScheduleRules;
import com.pashe.app.medicine.safety.MedicineSafetyService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UserMedicineService {

    private final UserMedicineRepository repository;
    private final MedicineCatalogService catalogService;
    private final MedicinePreferencesService preferencesService;
    private final MedicinePrescriptionService prescriptions;
    private final MedicineSafetyService safety;

    public UserMedicineService(
            UserMedicineRepository repository,
            MedicineCatalogService catalogService,
            MedicinePreferencesService preferencesService,
            MedicinePrescriptionService prescriptions,
            MedicineSafetyService safety) {

        this.repository = repository;
        this.catalogService = catalogService;
        this.preferencesService = preferencesService;
        this.prescriptions = prescriptions;
        this.safety = safety;
    }

    @Transactional
    public List<UserMedicine> getMyMedicines(UUID userId) {
        var medicines = repository.findByUserId(userId);
        var preferences = preferencesService.getPreferences(userId);
        var now = Instant.now();
        boolean changed = false;
        for (var medicine : medicines) {
            if (courseHasEnded(medicine, preferences, now)) {
                changed |= repository.finish(userId, medicine.id(), null, false) > 0;
            }
        }
        return changed ? repository.findByUserId(userId) : medicines;
    }

    static boolean courseHasEnded(UserMedicine medicine, MedicinePreferences preferences, Instant now) {
        if (!"ACTIVE".equals(medicine.status()) || medicine.durationDays() == null) {
            return false;
        }
        var endDate = medicine.startDate().plusDays(medicine.durationDays());
        var zone = ZoneId.of(preferences.timezone());
        if (now.isBefore(endDate.atStartOfDay(zone).toInstant())) {
            return false;
        }
        return MedicineScheduleRules.doses(medicine.dosePattern(), medicine.doseQuantity(), medicine.mealTiming(),
                preferences, endDate.minusDays(1)).stream()
            .noneMatch(dose -> dose.scheduledAt() != null && !now.isAfter(dose.scheduledAt().toInstant()));
    }

    @Transactional(readOnly = true)
    public MedicinePreview preview(UUID userId, UUID previousMedicineId, UserMedicineRequest request) {
        var preferences = preferencesService.getPreferences(userId);
        if (previousMedicineId != null) {
            validateReplacement(userId, previousMedicineId, request, preferences);
        }
        var validated = validate(userId, request);
        return new MedicinePreview(validated.name(), validated.pattern(), request.doseQuantity(), request.doseUnit().strip(),
            request.mealTiming(), request.startDate(), request.durationDays(),
            MedicineScheduleRules.doses(validated.pattern(), request.doseQuantity(), request.mealTiming(),
                preferences, request.startDate()),
            safety.preview(userId, previousMedicineId, request, validated.name(), validated.ingredients()));
    }

    @Transactional
    public UserMedicine replaceMedicine(UUID userId, UUID id, UserMedicineRequest request) {
        var preferences = preferencesService.getPreferences(userId);
        validateReplacement(userId, id, request, preferences);
        var validated = validate(userId, request);
        if (repository.finish(userId, id, request.startDate(), true) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Medicine is no longer active");
        }
        return insert(userId, request, validated, id);
    }

    private void validateReplacement(UUID userId, UUID id, UserMedicineRequest request,
                                     MedicinePreferences preferences) {
        var previous = findMedicine(userId, id);
        if (!"ACTIVE".equals(previous.status()) || courseHasEnded(previous, preferences, Instant.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Medicine is no longer active");
        }
        if (!request.startDate().equals(LocalDate.now(ZoneId.of(preferences.timezone())))
                || request.startDate().isBefore(previous.startDate())) {
            throw badRequest("A replacement must start today and on or after the original start date");
        }
    }

    @Transactional
    public UserMedicine addMedicine(
            UUID userId,
            UserMedicineRequest request) {

        return insert(userId, request, validate(userId, request), null);
    }

    private ValidatedMedicine validate(UUID userId, UserMedicineRequest request) {
        prescriptions.requireOwned(userId, request.prescriptionId());
        String customName = request.customMedicineName();

        if (customName != null) {
            customName = customName.strip();

            if (customName.isEmpty()) {
                customName = null;
            }
        }

        if ((request.medicineId() == null)
                == (customName == null)) {

            throw badRequest(
                "Choose a catalog medicine or enter a custom medicine"
            );
        }

        var catalog = request.medicineId() == null ? null : catalogService.getById(request.medicineId());
        String name = catalog == null ? customName : catalog.brandName();
        String pattern = normalizePattern(request.dosePattern(), request.asNeeded());
        return new ValidatedMedicine(name, pattern, catalog == null ? List.of() : catalog.ingredients());
    }

    private record ValidatedMedicine(String name, String pattern, List<Ingredient> ingredients) {}

    private UserMedicine insert(UUID userId, UserMedicineRequest request, ValidatedMedicine validated, UUID previousId) {
        UUID id = UUID.randomUUID();
        repository.insert(id, userId, request, request.medicineId() == null ? validated.name() : null,
            validated.pattern(), previousId);

        return findMedicine(userId, id);
    }

    @Transactional
    public UserMedicine stopMedicine(
            UUID userId,
            UUID id,
            String reason) {

        var medicine = findMedicine(userId, id);
        var preferences = preferencesService.getPreferences(userId);
        var today = LocalDate.now(ZoneId.of(preferences.timezone()));

        if (courseHasEnded(medicine, preferences, Instant.now())
                || repository.stop(userId, id, reason.strip(), today) == 0) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Medicine is no longer active"
            );
        }

        return findMedicine(userId, id);
    }

    private UserMedicine findMedicine(UUID userId, UUID id) {

        return repository.findById(userId, id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Medicine record not found"
            ));
    }

    static String normalizePattern(
            String pattern,
            boolean asNeeded) {

        if (asNeeded) {

            if (pattern != null && !pattern.isBlank()) {
                throw badRequest(
                    "As-needed medicines cannot have a dose pattern"
                );
            }

            return null;
        }

        if (pattern == null || pattern.isBlank()) {
            throw badRequest("Dose pattern is required");
        }

        // Convert Bangla digits to English digits.
        for (int i = 0; i < 10; i++) {
            pattern = pattern.replace(
                (char) ('০' + i),
                (char) ('0' + i)
            );
        }

        String[] slots = pattern.split("\\+", -1);

        if (slots.length != 3 && slots.length != 4) {
            throw badRequest(
                "Dose pattern must contain three or four slots"
            );
        }

        List<String> normalized = new ArrayList<>();
        boolean hasDose = false;

        for (String slot : slots) {

            String value = slot.strip();

            if (value.equals("½")) {
                value = "0.5";
            }

            if (!value.matches("[0-9]{1,2}(\\.[0-9]{1,3})?")) {
                throw badRequest("Invalid dose pattern");
            }

            BigDecimal quantity = new BigDecimal(value);

            if (quantity.signum() > 0) {
                hasDose = true;
            }

            normalized.add(
                quantity.stripTrailingZeros().toPlainString()
            );
        }

        if (!hasDose) {
            throw badRequest(
                "At least one dose must be scheduled"
            );
        }

        return String.join("+", normalized);
    }

    private static ResponseStatusException badRequest(String message) {

        return new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            message
        );
    }
}