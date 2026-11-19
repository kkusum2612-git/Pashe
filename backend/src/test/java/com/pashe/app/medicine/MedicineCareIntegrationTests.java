
package com.pashe.app.medicine;

import com.pashe.app.identity.UserService;
import com.pashe.app.medicine.safety.HealthPassportAllergyReader;
import com.pashe.app.medicine.safety.HealthPassportAllergyReader.Allergy;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MedicineCareIntegrationTests {
    private static final String BASE = "/api/v1/medicine";
    private static final String MEDICINES = BASE + "/my-medicines";
    private static final String PRESCRIPTIONS = BASE + "/prescriptions";
    private static final String SAFETY = BASE + "/safety";
    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Dhaka"));
    private static final AtomicInteger TEST_IP_SUFFIX = new AtomicInteger();

    @Autowired MockMvc mvc;
    @Autowired UserService users;
    @Autowired PasswordEncoder passwords;
    @Autowired JdbcTemplate db;
    @MockitoBean HealthPassportAllergyReader allergies;

    @Test
    void previewsAndSavesMedicinesWithPrivatePrescriptionAndRetainsReplacementHistory() throws Exception {
        var patient = account();
        var other = account();
        var photo = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", photo);
        mvc.perform(multipart(PRESCRIPTIONS)
                .file(new MockMultipartFile("image", "synthetic.png", "image/png", photo.toByteArray()))
                .param("doctorName", "Synthetic doctor").param("prescriptionDate", TODAY.toString())
                .cookie(patient.cookies()).with(csrf()))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.hasImage").value(true));
        var prescription = db.queryForObject("SELECT id FROM medicine_prescriptions WHERE user_id = ?", UUID.class, patient.id());

        mvc.perform(get(PRESCRIPTIONS + "/" + prescription + "/image").cookie(patient.cookies()))
            .andExpect(status().isOk()).andExpect(content().contentType("image/png"))
            .andExpect(header().string("Cache-Control", "no-store, private"));
        mvc.perform(get(PRESCRIPTIONS + "/" + prescription + "/image").cookie(other.cookies()))
            .andExpect(status().isNotFound());
        mvc.perform(get(PRESCRIPTIONS)).andExpect(status().isUnauthorized());

        var request = payload(null, "Synthetic custom", "১+০+½", TODAY.minusDays(1), "null", prescription);
        mvc.perform(post(MEDICINES + "/preview").cookie(patient.cookies()).with(csrf())
                .contentType("application/json").content(request))
            .andExpect(status().isOk()).andExpect(jsonPath("$.normalizedPattern").value("1+0+0.5"))
            .andExpect(jsonPath("$.doses[1].quantity").value(0.5))
            .andExpect(jsonPath("$.safety.warnings[0].kind").value("UNCHECKABLE"));
        mvc.perform(get(MEDICINES).cookie(patient.cookies())).andExpect(jsonPath("$").isEmpty());
        mvc.perform(post(MEDICINES).cookie(other.cookies()).with(csrf()).contentType("application/json").content(request))
            .andExpect(status().isNotFound());
        add(patient, request);
        var originalId = db.queryForObject("SELECT id FROM user_medicines WHERE user_id = ?", UUID.class, patient.id());

        var replacement = payload(null, "Synthetic custom", "0+0+1", TODAY, "null", prescription);
        mvc.perform(post(MEDICINES + "/" + originalId + "/replacement").cookie(patient.cookies()).with(csrf())
                .contentType("application/json").content(replacement))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.previousMedicineId").value(originalId.toString()));
        mvc.perform(get(MEDICINES).cookie(patient.cookies()))
            .andExpect(jsonPath("$[*].status", containsInAnyOrder("ACTIVE", "FINISHED")))
            .andExpect(jsonPath("$[1].prescriptionId").value(prescription.toString()));
        mvc.perform(get(BASE + "/schedule").param("date", TODAY.minusDays(1).toString()).cookie(patient.cookies()))
            .andExpect(jsonPath("$[0].userMedicineId").value(originalId.toString()));
        mvc.perform(get(BASE + "/schedule").param("date", TODAY.toString()).cookie(patient.cookies()))
            .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].slot").value("NIGHT"));
        mvc.perform(patch(MEDICINES + "/" + originalId + "/stop").cookie(other.cookies()).with(csrf())
                .contentType("application/json").content("{\"reason\":\"Synthetic reason\"}"))
            .andExpect(status().isNotFound());
        var replacementId = db.queryForObject("SELECT id FROM user_medicines WHERE previous_medicine_id = ?", UUID.class, originalId);
        mvc.perform(patch(MEDICINES + "/" + replacementId + "/stop").cookie(patient.cookies()).with(csrf())
                .contentType("application/json").content("{\"reason\":\"Synthetic reason\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("STOPPED"));

        add(patient, payload(null, "Expired synthetic course", "0+0+1", TODAY.minusDays(3), "1", null));
        mvc.perform(get(MEDICINES).cookie(patient.cookies()))
            .andExpect(jsonPath("$[?(@.medicineName == 'Expired synthetic course')].status").value("FINISHED"));
    }

    @Test
    void rechecksNewAllergiesAndPersistsWarningsAndIntentionalPairsPerPatient() throws Exception {
        var patient = account();
        var other = account();
        var alphaClass = UUID.randomUUID();
        var betaClass = UUID.randomUUID();
        db.update("INSERT INTO medicine_drug_classes (id, name) VALUES (?, 'Synthetic class A'), (?, 'Synthetic class B')",
            alphaClass, betaClass);
        db.update("INSERT INTO medicine_related_drug_classes VALUES ('synthetic class a', 'synthetic class b')");
        var alpha = ingredient("Synthetic Alpha", alphaClass);
        var beta = ingredient("Synthetic Beta", betaClass);
        var brand = catalog("Synthetic single");
        var combo = catalog("Synthetic combination");
        db.update("INSERT INTO medicine_catalog_ingredients VALUES (?, ?, 5, 'mg'), (?, ?, 20, 'mg'), (?, ?, 10, 'mg')",
            brand, alpha, combo, alpha, combo, beta);
        mvc.perform(get(BASE + "/catalog").param("query", "Synthetic Beta").cookie(patient.cookies()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].ingredients.length()").value(2));

        when(allergies.findAllergies(patient.id())).thenReturn(Optional.of(List.of()));
        add(patient, payload(brand, null, "1+0+1", TODAY, "null", null));
        var comboPayload = payload(combo, null, "1+0+1", TODAY, "null", null);
        mvc.perform(post(MEDICINES + "/preview").cookie(patient.cookies()).with(csrf())
                .contentType("application/json").content(comboPayload))
            .andExpect(jsonPath("$.safety.warnings[0].kind").value("DUPLICATE_INGREDIENT"));
        add(patient, comboPayload);
        var warning = mvc.perform(get(SAFETY).cookie(patient.cookies())).andExpect(status().isOk())
            .andExpect(jsonPath("$.warnings[0].kind").value("DUPLICATE_INGREDIENT")).andReturn()
            .getResponse().getContentAsString();
        String warningId = com.jayway.jsonpath.JsonPath.read(warning, "$.warnings[0].id");
        String acknowledgement = SAFETY + "/" + warningId + "/acknowledgement";

        mvc.perform(put(acknowledgement).cookie(other.cookies()).with(csrf())
                .contentType("application/json").content("{\"intentional\":false}"))
            .andExpect(status().isNotFound());
        mvc.perform(put(acknowledgement).cookie(patient.cookies()).with(csrf())
                .contentType("application/json").content("{\"intentional\":false}"))
            .andExpect(status().isNoContent());
        mvc.perform(get(SAFETY).cookie(patient.cookies()))
            .andExpect(jsonPath("$.warnings[0].acknowledgedAt").isNotEmpty());
        assertEquals(patient.id(), db.queryForObject(
            "SELECT acknowledged_by FROM medicine_safety_acknowledgements WHERE warning_id = ?", UUID.class, warningId));
        mvc.perform(put(acknowledgement).cookie(patient.cookies()).with(csrf())
                .contentType("application/json").content("{\"intentional\":true}"))
            .andExpect(status().isNoContent());
        mvc.perform(get(SAFETY).cookie(patient.cookies())).andExpect(jsonPath("$.warnings").isEmpty());

        when(allergies.findAllergies(patient.id())).thenReturn(Optional.of(List.of(
            new Allergy(UUID.randomUUID(), "Synthetic class A", null),
            new Allergy(UUID.randomUUID(), null, "Reported Synthetic Beta")
        )));
        mvc.perform(get(SAFETY).cookie(patient.cookies())).andExpect(jsonPath("$.warnings[*].kind",
            containsInAnyOrder("EXACT_CLASS", "EXACT_CLASS", "RELATED_CLASS", "POSSIBLE_TEXT")));
    }

    @Test
    void savesRamadanPreferencesAndUpdatesTheScheduleWithoutChangingDoseIdentity() throws Exception {
        var patient = account();
        add(patient, payload(null, "Synthetic routine test", "1+1+1+1", TODAY, "null", null));
        var before = mvc.perform(get(BASE + "/schedule").param("date", TODAY.toString()).cookie(patient.cookies()))
            .andExpect(jsonPath("$[0].scheduledAt").value(TODAY + "T08:15:00+06:00"))
            .andReturn().getResponse().getContentAsString();
        String morningId = com.jayway.jsonpath.JsonPath.read(before, "$[0].doseId");

        String preferences = """
            {"breakfastTime":"09:00","lunchTime":"13:00","dinnerTime":"20:00","bedtime":"22:30",
             "timezone":"Asia/Dhaka","ramadan":{"startDate":"%s","endDate":"%s","sehriTime":"04:30","iftarTime":"18:00"}}
            """.formatted(TODAY, TODAY);
        mvc.perform(put(BASE + "/preferences").cookie(patient.cookies()).with(csrf())
                .contentType("application/json").content(preferences))
            .andExpect(status().isOk());
        mvc.perform(get(BASE + "/preferences").cookie(patient.cookies()))
            .andExpect(jsonPath("$.ramadan.startDate").value(TODAY.toString()));
        mvc.perform(get(BASE + "/schedule").param("date", TODAY.toString()).cookie(patient.cookies()))
            .andExpect(jsonPath("$[0].doseId").value(morningId))
            .andExpect(jsonPath("$[0].scheduledAt").value(TODAY + "T04:45:00+06:00"))
            .andExpect(jsonPath("$[3].slot").value("NOON"))
            .andExpect(jsonPath("$[3].fastingConflict").value(true));
        mvc.perform(get(BASE + "/schedule").param("date", TODAY.plusDays(1).toString()).cookie(patient.cookies()))
            .andExpect(jsonPath("$[0].scheduledAt").value(TODAY.plusDays(1) + "T09:15:00+06:00"))
            .andExpect(jsonPath("$[1].fastingConflict").value(false));
    }

    private UUID ingredient(String name, UUID drugClass) {
        UUID id = UUID.randomUUID();
        db.update("INSERT INTO medicine_ingredients (id, name, drug_class_id) VALUES (?, ?, ?)", id, name, drugClass);
        return id;
    }

    private UUID catalog(String brand) {
        UUID id = UUID.randomUUID();
        db.update("INSERT INTO medicine_catalog (id, brand_name, manufacturer, form, mrp) VALUES (?, ?, 'Synthetic', 'TABLET', 1)",
            id, brand);
        return id;
    }

    private void add(Account account, String payload) throws Exception {
        mvc.perform(post(MEDICINES).cookie(account.cookies()).with(csrf()).contentType("application/json").content(payload))
            .andExpect(status().isCreated());
    }

    private String payload(UUID catalog, String custom, String pattern, LocalDate start, String days, UUID prescription) {
        return """
            {"medicineId":%s,"customMedicineName":%s,"doseQuantity":1,"doseUnit":"tablet","dosePattern":"%s",
             "mealTiming":"AFTER_MEAL","asNeeded":false,"critical":false,"startDate":"%s",
             "durationDays":%s,"prescriptionId":%s}
            """.formatted(quote(catalog), quote(custom), pattern, start, days, quote(prescription));
    }

    private String quote(Object value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private Account account() throws Exception {
        String email = UUID.randomUUID() + "@example.test";
        String password = "Synthetic-Password-2026!";
        UUID id = users.create("Synthetic patient", email, "01712345678", passwords.encode(password), true);
        String remoteAddress = "192.0.2." + TEST_IP_SUFFIX.incrementAndGet();
        Cookie[] cookies = mvc.perform(post("/api/v1/auth/login").with(csrf()).with(request -> {
                    request.setRemoteAddr(remoteAddress);
                    return request;
                }).contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getCookies();
        return new Account(id, cookies);
    }

    private record Account(UUID id, Cookie[] cookies) {}
}
