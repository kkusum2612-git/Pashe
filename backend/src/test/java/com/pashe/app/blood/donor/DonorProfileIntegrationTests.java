package com.pashe.app.blood.donor;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.pashe.app.identity.UserService;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DonorProfileIntegrationTests {

    private static final String URL =
        "/api/v1/blood/donor-profile";

    private static final String PASSWORD =
        "Test-Password-2026!";

    @Autowired
    MockMvc mvc;

    @Autowired
    UserService users;

    @Autowired
    PasswordEncoder passwords;

    @Autowired
    JdbcTemplate db;

    private record TestAccount(UUID id, Cookie[] cookies) {}

    private TestAccount createAccount() throws Exception {
        String email = UUID.randomUUID() + "@example.test";

        UUID id = users.create(
            "Test Donor",
            email,
            "01712345678",
            passwords.encode(PASSWORD),
            true
        );

        Cookie[] cookies = mvc.perform(
            post("/api/v1/auth/login")
                .with(csrf())
                .contentType("application/json")
                .content("""
                    {
                        "email": "%s",
                        "password": "%s"
                    }
                    """.formatted(email, PASSWORD))
        )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getCookies();

        return new TestAccount(id, cookies);
    }

    private String profile(
        String bloodGroup,
        String area,
        String lastDonationDate,
        boolean available
    ) {
        String date = lastDonationDate == null
            ? "null"
            : "\"" + lastDonationDate + "\"";

        return """
            {
                "bloodGroup": "%s",
                "area": "%s",
                "lastDonationDate": %s,
                "available": %s
            }
            """.formatted(
                bloodGroup,
                area,
                date,
                available
            );
    }

    private ResultActions save(
        TestAccount account,
        String body
    ) throws Exception {
        return mvc.perform(
            put(URL)
                .cookie(account.cookies())
                .with(csrf())
                .contentType("application/json")
                .content(body)
        );
    }

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mvc.perform(get(URL))
            .andExpect(status().isUnauthorized());

        mvc.perform(
            put(URL)
                .with(csrf())
                .contentType("application/json")
                .content(profile("B+", "Mirpur", null, true))
        )
            .andExpect(status().isUnauthorized());
    }

    @Test
    void nonexistentProfileReturnsNotFound() throws Exception {
        TestAccount account = createAccount();

        mvc.perform(get(URL).cookie(account.cookies()))
            .andExpect(status().isNotFound());
    }

    @Test
    void createsAndRetrievesDonorProfile() throws Exception {
        TestAccount account = createAccount();

        save(
            account,
            profile("B+", "Mirpur, Dhaka", "2020-01-15", true)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(
                account.id().toString()
            ))
            .andExpect(jsonPath("$.bloodGroup").value("B+"))
            .andExpect(jsonPath("$.area").value("Mirpur, Dhaka"))
            .andExpect(jsonPath("$.available").value(true));

        mvc.perform(get(URL).cookie(account.cookies()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bloodGroup").value("B+"))
            .andExpect(jsonPath("$.lastDonationDate").value(
                "2020-01-15"
            ));

        assertEquals(
            "B+",
            db.queryForObject(
                """
                SELECT blood_group
                FROM blood_donor_profiles
                WHERE user_id = ?
                """,
                String.class,
                account.id()
            )
        );
    }

    @Test
    void updatesExistingProfileWithoutDuplicates() throws Exception {
        TestAccount account = createAccount();

        save(
            account,
            profile("O+", "Mirpur", "2020-01-15", true)
        )
            .andExpect(status().isOk());

        save(
            account,
            profile("B+", "Dhanmondi", null, false)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bloodGroup").value("B+"))
            .andExpect(jsonPath("$.area").value("Dhanmondi"))
            .andExpect(jsonPath("$.available").value(false))
            .andExpect(jsonPath("$.lastDonationDate").doesNotExist());

        assertEquals(
            Long.valueOf(1),
            db.queryForObject(
                """
                SELECT COUNT(*)
                FROM blood_donor_profiles
                WHERE user_id = ?
                """,
                Long.class,
                account.id()
            )
        );
    }

    @Test
    void invalidProfilesAreRejected() throws Exception {
        TestAccount account = createAccount();

        save(account, profile("X+", "Mirpur", null, true))
            .andExpect(status().isBadRequest());

        save(account, profile("B+", "   ", null, true))
            .andExpect(status().isBadRequest());

        save(
            account,
            profile(
                "B+",
                "Mirpur",
                LocalDate.now().plusYears(1).toString(),
                true
            )
        )
            .andExpect(status().isBadRequest());

        save(account, """
            {
                "bloodGroup": "B+",
                "area": "Mirpur"
            }
            """)
            .andExpect(status().isBadRequest());

        mvc.perform(get(URL).cookie(account.cookies()))
            .andExpect(status().isNotFound());
    }

    @Test
    void usersCanOnlyAccessTheirOwnProfiles() throws Exception {
        TestAccount first = createAccount();
        TestAccount second = createAccount();

        save(first, profile("B+", "Mirpur", null, true))
            .andExpect(status().isOk());

        mvc.perform(get(URL).cookie(second.cookies()))
            .andExpect(status().isNotFound());

        save(second, profile("O-", "Uttara", null, false))
            .andExpect(status().isOk());

        mvc.perform(get(URL).cookie(first.cookies()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(
                first.id().toString()
            ))
            .andExpect(jsonPath("$.bloodGroup").value("B+"));

        mvc.perform(get(URL).cookie(second.cookies()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(
                second.id().toString()
            ))
            .andExpect(jsonPath("$.bloodGroup").value("O-"));
    }

    @Test
    void savingWithoutCsrfTokenIsRejected() throws Exception {
        TestAccount account = createAccount();

        mvc.perform(
            put(URL)
                .cookie(account.cookies())
                .contentType("application/json")
                .content(profile("B+", "Mirpur", null, true))
        )
            .andExpect(status().isForbidden());
    }
}