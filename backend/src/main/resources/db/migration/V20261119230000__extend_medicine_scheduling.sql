
CREATE TABLE medicine_prescriptions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    doctor_name VARCHAR(150) NOT NULL CHECK (LENGTH(TRIM(doctor_name)) > 0),
    prescription_date DATE NOT NULL,
    image BYTEA,
    image_type VARCHAR(20),
    UNIQUE (user_id, id),
    CHECK ((image IS NULL) = (image_type IS NULL))
);

ALTER TABLE user_medicines
    ADD COLUMN prescription_id UUID,
    ADD COLUMN previous_medicine_id UUID UNIQUE,
    ADD COLUMN schedule_end_date DATE,
    ADD CONSTRAINT fk_user_medicine_prescription
        FOREIGN KEY (user_id, prescription_id) REFERENCES medicine_prescriptions(user_id, id),
    ADD CONSTRAINT uq_user_medicine_owner_id UNIQUE (user_id, id),
    ADD CONSTRAINT fk_user_medicine_previous
        FOREIGN KEY (user_id, previous_medicine_id) REFERENCES user_medicines(user_id, id);

ALTER TABLE medicine_preferences
    ADD COLUMN ramadan_start DATE,
    ADD COLUMN ramadan_end DATE,
    ADD COLUMN sehri_time TIME,
    ADD COLUMN iftar_time TIME,
    ADD CONSTRAINT chk_medicine_ramadan CHECK (
        num_nonnulls(ramadan_start, ramadan_end, sehri_time, iftar_time) = 0
        OR (num_nonnulls(ramadan_start, ramadan_end, sehri_time, iftar_time) = 4
            AND ramadan_start <= ramadan_end)
    );
