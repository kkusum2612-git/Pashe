package com.pashe.app.blood.request;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record BloodRequestCreateRequest(

    @NotBlank
    @Pattern(regexp = "(A|B|AB|O)[+-]")
    String bloodGroup,

    @NotNull
    @Positive
    Integer requiredUnits,

    @NotBlank
    @Size(max = 200)
    String hospitalName,

    @NotBlank
    @Pattern(regexp = "NORMAL|URGENT|CRITICAL")
    String urgency,

    @NotNull
    @Future
    OffsetDateTime expiresAt

) {}