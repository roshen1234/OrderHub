package com.orderhub.order.DTO;

import com.orderhub.order.entity.ShippingAddress;

public record ShippingAddressResponse(String name, String phone, String line1, String line2,
                                      String city, String state, String postalCode, String country) {
    public static ShippingAddressResponse from(ShippingAddress s) {
        if (s == null) return null;                                   // orders from before this change
        return new ShippingAddressResponse(s.getName(), s.getPhone(), s.getLine1(), s.getLine2(),
                s.getCity(), s.getState(), s.getPostalCode(), s.getCountry());
    }
}
