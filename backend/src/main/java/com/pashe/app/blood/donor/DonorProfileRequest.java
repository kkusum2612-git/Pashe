package com.pashe.app.blood.donor;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record DonorProfileRequest(

    @NotBlank
    @Pattern(regexp = "^(A|B|AB|O)[+-]$")
    String bloodGroup,

    @NotBlank
    @Size(max = 120)
    String area,

    @PastOrPresent
    LocalDate lastDonationDate,

    @NotNull
    Boolean available

) {}