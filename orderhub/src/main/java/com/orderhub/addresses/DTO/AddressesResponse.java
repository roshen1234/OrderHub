package com.orderhub.addresses.DTO;

import com.orderhub.addresses.Addresses;

import java.time.LocalDateTime;

public record AddressesResponse(Long id, String label, String recipientName, String phone,
                              String line1, String line2, String city, String state,
                              String postalCode, String country, boolean defaultAddress,
                              LocalDateTime createdAt) {
    public static AddressesResponse from(Addresses a) {
        return new AddressesResponse(a.getId(), a.getLabel(), a.getRecipientName(), a.getPhone(),
                a.getLine1(), a.getLine2(), a.getCity(), a.getState(), a.getPostalCode(),
                a.getCountry(), a.isDefaultAddress(), a.getCreatedAt());
    }
}
