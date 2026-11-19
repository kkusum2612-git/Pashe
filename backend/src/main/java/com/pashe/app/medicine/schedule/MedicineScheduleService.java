
package com.pashe.app.medicine.schedule;

import com.pashe.app.medicine.medication.UserMedicine;
import com.pashe.app.medicine.medication.UserMedicineService;
import com.pashe.app.medicine.preferences.MedicinePreferencesService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class MedicineScheduleService {
    private final UserMedicineService medicineService;
    private final MedicinePreferencesService preferencesService;

    public MedicineScheduleService(UserMedicineService medicineService, MedicinePreferencesService preferencesService) {
        this.medicineService = medicineService;
        this.preferencesService = preferencesService;
    }

    public List<ScheduledDose> getSchedule(UUID userId, LocalDate requestedDate) {
        var preferences = preferencesService.getPreferences(userId);
        var zone = ZoneId.of(preferences.timezone());
        LocalDate date = requestedDate == null ? LocalDate.now(zone) : requestedDate;
        List<ScheduledDose> doses = new ArrayList<>();

        for (UserMedicine medicine : medicineService.getMyMedicines(userId)) {
            if (!isScheduledOn(medicine, date, zone)) {
                continue;
            }
            for (var dose : MedicineScheduleRules.doses(medicine.dosePattern(), medicine.doseQuantity(),
                    medicine.mealTiming(), preferences, date)) {
                doses.add(new ScheduledDose(
                    medicine.id() + ":" + date + ":" + dose.slot(), medicine.id(), medicine.medicineName(),
                    date, dose.slot().name(), dose.scheduledAt(), dose.quantity(), medicine.doseUnit(),
                    medicine.mealTiming(), medicine.critical(), dose.fastingConflict()
                ));
            }
        }

        doses.sort(Comparator.comparing(dose -> dose.scheduledAt() == null
            ? null : dose.scheduledAt().toInstant(), Comparator.nullsLast(Comparator.naturalOrder())));
        return doses;
    }

    private boolean isScheduledOn(UserMedicine medicine, LocalDate date, ZoneId zone) {
        if (date.isBefore(medicine.startDate()) || medicine.asNeeded()) {
            return false;
        }
        LocalDate endDate = medicine.scheduleEndDate();
        if (endDate == null && "STOPPED".equals(medicine.status()) && medicine.endedAt() != null) {
            endDate = medicine.endedAt().atZoneSameInstant(zone).toLocalDate();
        }
        if (endDate != null && !date.isBefore(endDate)) {
            return false;
        }
        return medicine.durationDays() == null || date.isBefore(medicine.startDate().plusDays(medicine.durationDays()));
    }
}
