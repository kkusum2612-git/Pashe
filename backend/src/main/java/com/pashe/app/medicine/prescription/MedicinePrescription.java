
package com.pashe.app.medicine.prescription;

import java.time.LocalDate;
import java.util.UUID;

public record MedicinePrescription(UUID id, String doctorName, LocalDate prescriptionDate, boolean hasImage) {}
