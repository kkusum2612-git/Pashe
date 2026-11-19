CREATE TABLE medicine_preferences (

    user_id UUID PRIMARY KEY
        REFERENCES users(id) ON DELETE CASCADE,

    breakfast_time TIME NOT NULL DEFAULT '08:00:00',

    lunch_time TIME NOT NULL DEFAULT '13:00:00',

    dinner_time TIME NOT NULL DEFAULT '20:00:00',

    bedtime TIME NOT NULL DEFAULT '22:30:00',

    timezone VARCHAR(60) NOT NULL DEFAULT 'Asia/Dhaka',

    created_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP
);