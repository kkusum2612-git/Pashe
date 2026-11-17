package com.pashe.app.ambulance.profile;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AmbulanceProfileRepository
        extends JpaRepository<AmbulanceProfile, UUID> {

    List<AmbulanceProfile> findByOperatorIdOrderByCreatedAtDesc(
            UUID operatorId
    );

    List<AmbulanceProfile> findByVerificationStatusOrderByCreatedAtDesc(
            VerificationStatus status
    );

    boolean existsByRegistrationNumberIgnoreCase(
            String registrationNumber
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AmbulanceProfile a WHERE a.id = :id")
    Optional<AmbulanceProfile> findByIdForUpdate(
            @Param("id") UUID id
    );
}