
package com.pashe.app.medicine.safety;

import com.pashe.app.medicine.catalog.MedicineCatalogItem.Ingredient;
import com.pashe.app.medicine.medication.UserMedicineRepository;
import com.pashe.app.medicine.medication.UserMedicineRequest;
import com.pashe.app.medicine.preferences.MedicinePreferencesService;
import com.pashe.app.medicine.safety.HealthPassportAllergyReader.Allergy;
import com.pashe.app.medicine.safety.MedicineSafetyReport.Kind;
import com.pashe.app.medicine.safety.MedicineSafetyReport.Warning;
import com.pashe.app.medicine.safety.MedicineSafetyRepository.MedicinePair;
import com.pashe.app.medicine.safety.MedicineSafetyRules.Medicine;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
public class MedicineSafetyService {
    private final MedicineSafetyRepository repository;
    private final UserMedicineRepository medicines;
    private final MedicinePreferencesService preferences;
    private final ObjectProvider<HealthPassportAllergyReader> allergyReader;

    MedicineSafetyService(MedicineSafetyRepository repository, UserMedicineRepository medicines,
                          MedicinePreferencesService preferences, ObjectProvider<HealthPassportAllergyReader> allergyReader) {
        this.repository = repository;
        this.medicines = medicines;
        this.preferences = preferences;
        this.allergyReader = allergyReader;
    }

    @Transactional(readOnly = true)
    public MedicineSafetyReport list(UUID userId) {
        return check(userId, null, null);
    }

    @Transactional(readOnly = true)
    public MedicineSafetyReport preview(UUID userId, UUID previousId, UserMedicineRequest request, String name,
                                        List<Ingredient> ingredients) {
        var candidate = new Medicine(null, name, request.startDate(),
            endDate(request.startDate(), request.durationDays()), ingredients);
        return check(userId, previousId, candidate);
    }

    @Transactional
    public void acknowledge(UUID userId, String warningId, boolean intentional) {
        var warning = check(userId, null, null).warnings().stream()
            .filter(item -> item.id().equals(warningId)).findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Current warning not found"));
        if (intentional) {
            if (warning.kind() != Kind.DUPLICATE_INGREDIENT) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only duplicate warnings support confirmation");
            }
            repository.confirmPair(userId, MedicinePair.of(warning.medicineId(), warning.otherMedicineId()));
        }
        repository.acknowledge(userId, warning.id());
    }

    private MedicineSafetyReport check(UUID userId, UUID previousId, Medicine candidate) {
        Optional<List<Allergy>> allergies = Optional.ofNullable(allergyReader.getIfAvailable())
            .flatMap(reader -> reader.findAllergies(userId));
        var related = allergies.isPresent() ? repository.findRelatedClasses() : Set.<MedicineSafetyRepository.RelatedClasses>of();
        var acknowledged = candidate == null ? repository.findAcknowledgements(userId) : Map.<String, java.time.OffsetDateTime>of();
        var warnings = new ArrayList<Warning>();
        var active = activeMedicines(userId, previousId);

        if (candidate != null) {
            warnings.addAll(MedicineSafetyRules.allergies(candidate, allergies.orElseGet(List::of), related, acknowledged));
            for (var other : active) {
                warnings.addAll(MedicineSafetyRules.duplicates(candidate, other, acknowledged));
            }
        } else {
            var intentional = repository.findIntentionalPairs(userId);
            for (int i = 0; i < active.size(); i++) {
                var medicine = active.get(i);
                warnings.addAll(MedicineSafetyRules.allergies(medicine, allergies.orElseGet(List::of), related, acknowledged));
                for (var other : active.subList(i + 1, active.size())) {
                    if (!intentional.contains(MedicinePair.of(medicine.id(), other.id()))) {
                        warnings.addAll(MedicineSafetyRules.duplicates(medicine, other, acknowledged));
                    }
                }
            }
        }
        return new MedicineSafetyReport(allergies.isPresent(), List.copyOf(warnings));
    }

    private List<Medicine> activeMedicines(UUID userId, UUID previousId) {
        var today = LocalDate.now(ZoneId.of(preferences.getPreferences(userId).timezone()));
        var ingredients = repository.findIngredients(userId);
        var active = new ArrayList<Medicine>();
        for (var medicine : medicines.findByUserId(userId)) {
            if (!"ACTIVE".equals(medicine.status()) || medicine.id().equals(previousId)) {
                continue;
            }
            var end = endDate(medicine.startDate(), medicine.durationDays());
            if (end == null || end.isAfter(today)) {
                active.add(new Medicine(medicine.id(), medicine.medicineName(), medicine.startDate(), end,
                    medicine.medicineId() == null ? List.of()
                        : ingredients.getOrDefault(medicine.medicineId(), List.of())));
            }
        }
        return active;
    }

    private LocalDate endDate(LocalDate start, Integer days) {
        return days == null ? null : start.plusDays(days);
    }
}
