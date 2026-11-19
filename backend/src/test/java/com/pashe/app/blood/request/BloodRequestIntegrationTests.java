package com.pashe.app.blood.request;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.pashe.app.identity.UserService;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BloodRequestIntegrationTests {

    private static final String URL =
        "/api/v1/blood/requests";

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

    @Autowired
    BloodRequestStatusScheduler scheduler;

    private record TestAccount(UUID id, Cookie[] cookies) {}

    private TestAccount createAccount() throws Exception {
        String email = UUID.randomUUID() + "@example.test";

        UUID id = users.create(
            "Test Requester",
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

    private String body(
        String group,
        int units,
        String hospital,
        String urgency,
        OffsetDateTime deadline
    ) {
        return """
            {
                "bloodGroup": "%s",
                "requiredUnits": %d,
                "hospitalName": "%s",
                "urgency": "%s",
                "expiresAt": "%s"
            }
            """.formatted(
                group,
                units,
                hospital,
                urgency,
                deadline
            );
    }

    private UUID createRequest(
        TestAccount account,
        String hospital,
        int units
    ) throws Exception {

        mvc.perform(
            post(URL)
                .cookie(account.cookies())
                .with(csrf())
                .contentType("application/json")
                .content(body(
                    "B+",
                    units,
                    hospital,
                    "URGENT",
                    OffsetDateTime.now().plusDays(1)
                ))
        )
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.hospitalName").value(hospital))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.acceptedDonorsCount").value(0))
            .andExpect(jsonPath("$.confirmedUnits").value(0));

        return db.queryForObject("""
            SELECT id
            FROM blood_requests
            WHERE requester_user_id = ?
              AND hospital_name = ?
            """,
            UUID.class,
            account.id(),
            hospital
        );
    }

    @Test
    void createsAndRetrievesRequest() throws Exception {
        TestAccount account = createAccount();

        UUID id = createRequest(
            account,
            "FR12 Integration Hospital",
            3
        );

        mvc.perform(
            get(URL + "/" + id)
                .cookie(account.cookies())
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.bloodGroup").value("B+"))
            .andExpect(jsonPath("$.requiredUnits").value(3))
            .andExpect(jsonPath("$.confirmedUnits").value(0))
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        mvc.perform(
            get(URL).cookie(account.cookies())
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(id.toString()));

        assertEquals(
            "ACTIVE",
            db.queryForObject(
                "SELECT status FROM blood_requests WHERE id = ?",
                String.class,
                id
            )
        );
    }

    @Test
    void rejectsInvalidRequests() throws Exception {
        TestAccount account = createAccount();

        String[] invalidBodies = {
            body(
                "X+",
                3,
                "Test Hospital",
                "URGENT",
                OffsetDateTime.now().plusDays(1)
            ),
            body(
                "B+",
                0,
                "Test Hospital",
                "URGENT",
                OffsetDateTime.now().plusDays(1)
            ),
            body(
                "B+",
                3,
                "   ",
                "URGENT",
                OffsetDateTime.now().plusDays(1)
            ),
            body(
                "B+",
                3,
                "Test Hospital",
                "URGENT",
                OffsetDateTime.now().minusDays(1)
            ),
            body(
                "B+",
                3,
                "Test Hospital",
                "INVALID",
                OffsetDateTime.now().plusDays(1)
            )
        };

        for (String invalidBody : invalidBodies) {
            mvc.perform(
                post(URL)
                    .cookie(account.cookies())
                    .with(csrf())
                    .contentType("application/json")
                    .content(invalidBody)
            )
                .andExpect(status().isBadRequest());
        }

        assertEquals(
            Long.valueOf(0),
            db.queryForObject("""
                SELECT COUNT(*)
                FROM blood_requests
                WHERE requester_user_id = ?
                """,
                Long.class,
                account.id()
            )
        );
    }

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mvc.perform(get(URL))
            .andExpect(status().isUnauthorized());

        mvc.perform(
            post(URL)
                .with(csrf())
                .contentType("application/json")
                .content(body(
                    "O+",
                    1,
                    "Test Hospital",
                    "NORMAL",
                    OffsetDateTime.now().plusDays(1)
                ))
        )
            .andExpect(status().isUnauthorized());
    }

    @Test
    void requestsWithoutCsrfAreRejected() throws Exception {
        TestAccount account = createAccount();

        mvc.perform(
            post(URL)
                .cookie(account.cookies())
                .contentType("application/json")
                .content(body(
                    "B+",
                    2,
                    "Test Hospital",
                    "URGENT",
                    OffsetDateTime.now().plusDays(1)
                ))
        )
            .andExpect(status().isForbidden());
    }

    @Test
    void usersCannotAccessOtherUsersRequests() throws Exception {
        TestAccount first = createAccount();
        TestAccount second = createAccount();

        UUID firstId = createRequest(
            first,
            "First User Hospital",
            2
        );

        mvc.perform(
            get(URL + "/" + firstId)
                .cookie(second.cookies())
        )
            .andExpect(status().isNotFound());

        mvc.perform(
            get(URL).cookie(second.cookies())
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());

        UUID secondId = createRequest(
            second,
            "Second User Hospital",
            1
        );

        mvc.perform(
            get(URL).cookie(second.cookies())
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(secondId.toString()));

        mvc.perform(
            get(URL + "/" + secondId)
                .cookie(first.cookies())
        )
            .andExpect(status().isNotFound());
    }

    @Test
    void progressUpdatesAreVisibleThroughApi() throws Exception {
        TestAccount account = createAccount();

        UUID id = createRequest(
            account,
            "Progress Test Hospital",
            3
        );

        db.update("""
            UPDATE blood_requests
            SET accepted_donors_count = 2,
                confirmed_units = 1,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """,
            id
        );

        mvc.perform(
            get(URL + "/" + id)
                .cookie(account.cookies())
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.acceptedDonorsCount").value(2))
            .andExpect(jsonPath("$.confirmedUnits").value(1))
            .andExpect(jsonPath("$.requiredUnits").value(3))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void completedRequestsBecomeFulfilled() throws Exception {
        TestAccount account = createAccount();

        UUID id = createRequest(
            account,
            "Fulfillment Test Hospital",
            3
        );

        db.update("""
            UPDATE blood_requests
            SET accepted_donors_count = 3,
                confirmed_units = 3,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """,
            id
        );

        scheduler.reconcileActiveRequests();

        assertEquals(
            "FULFILLED",
            db.queryForObject(
                "SELECT status FROM blood_requests WHERE id = ?",
                String.class,
                id
            )
        );

        mvc.perform(
            get(URL + "/" + id)
                .cookie(account.cookies())
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.confirmedUnits").value(3))
            .andExpect(jsonPath("$.status").value("FULFILLED"));
    }

    @Test
    void overdueRequestsBecomeExpired() throws Exception {
        TestAccount account = createAccount();

        UUID id = createRequest(
            account,
            "Expiration Test Hospital",
            2
        );

        db.update("""
            UPDATE blood_requests
            SET created_at = CURRENT_TIMESTAMP - INTERVAL '2 hours',
                expires_at = CURRENT_TIMESTAMP - INTERVAL '1 hour'
            WHERE id = ?
            """,
            id
        );

        scheduler.reconcileActiveRequests();

        assertEquals(
            "EXPIRED",
            db.queryForObject(
                "SELECT status FROM blood_requests WHERE id = ?",
                String.class,
                id
            )
        );

        mvc.perform(
            get(URL + "/" + id)
                .cookie(account.cookies())
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("EXPIRED"));
    }
}