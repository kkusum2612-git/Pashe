package com.pashe.app.ambulance.profile;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "ambulance_equipment")
public class AmbulanceEquipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ambulance_id", nullable = false)
    private UUID ambulanceId;

    @Column(name = "equipment_name", nullable = false, length = 100)
    private String equipmentName;

    @Column(nullable = false)
    private int quantity;

    protected AmbulanceEquipment() {
    }

    public AmbulanceEquipment(
            UUID ambulanceId,
            String equipmentName,
            int quantity
    ) {
        this.ambulanceId = ambulanceId;
        this.equipmentName = equipmentName;
        this.quantity = quantity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAmbulanceId() {
        return ambulanceId;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public int getQuantity() {
        return quantity;
    }
}