
package com.pashe.app.medicine.medication;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Component
class MedicineCourseScheduler {
    private final UserMedicineRepository repository;
    private final UserMedicineService service;

    MedicineCourseScheduler(UserMedicineRepository repository, UserMedicineService service) {
        this.repository = repository;
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${pashe.medicine.course-sweep-delay-ms:60000}")
    public void finishExpiredCourses() {
        var throughDate = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        for (var userId : repository.findOwnersWithCoursesNearEnd(throughDate)) {
            service.getMyMedicines(userId);
        }
    }
}
