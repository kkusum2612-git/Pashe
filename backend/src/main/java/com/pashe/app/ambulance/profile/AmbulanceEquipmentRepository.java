package com.pashe.app.ambulance.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AmbulanceEquipmentRepository
        extends JpaRepository<AmbulanceEquipment, UUID> {

    List<AmbulanceEquipment> findByAmbulanceIdOrderByEquipmentNameAsc(
            UUID ambulanceId
    );

    boolean existsByAmbulanceIdAndEquipmentNameIgnoreCase(
            UUID ambulanceId,
            String equipmentName
    );

    Optional<AmbulanceEquipment> findByIdAndAmbulanceId(
            UUID id,
            UUID ambulanceId
    );
}