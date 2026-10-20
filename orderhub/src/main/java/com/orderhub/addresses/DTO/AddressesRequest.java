package com.orderhub.addresses.DTO;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CurrentTimestamp;

public record AddressesRequest(
        @Size(max = 50) String label,
        @NotBlank @Size(max = 100) String recipientName,
        @NotBlank @Pattern(regexp = "^\\+?[0-9 ()-]{7,20}$", message = "Invalid phone number") String phone,
        @NotBlank @Size(max = 200) String line1,
        @Size(max = 200) String line2,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 100) String state,
        @NotBlank @Size(max = 20) String postalCode,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$", message = "Country must be a 2-letter code") String country,
        Boolean makeDefault
) {}
