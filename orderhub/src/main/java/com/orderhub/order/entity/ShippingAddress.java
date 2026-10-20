package com.orderhub.order.entity;

import com.orderhub.addresses.Addresses;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

//these are not seperate Entity these are just fields that is present in main table "order" we just sepearelty mention them here so we can reuse

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class ShippingAddress {

    @Column(name = "ship_name", length = 100)
    private String name;

    @Column(name = "ship_phone", length = 20)
    private String phone;

    @Column(name = "ship_line1", length = 200)
    private String line1;

    @Column(name = "ship_line2", length = 200)
    private String line2;

    @Column(name = "ship_city", length = 100)
    private String city;

    @Column(name = "ship_state", length = 100)
    private String state;

    @Column(name = "ship_postal_code", length = 20)
    private String postalCode;

    @Column(name = "ship_country", length = 2)
    private String country;

    public static ShippingAddress from(Addresses a) {            // copies the values, keeps no link to the row
        ShippingAddress s = new ShippingAddress();
        s.name = a.getRecipientName();
        s.phone = a.getPhone();
        s.line1 = a.getLine1();
        s.line2 = a.getLine2();
        s.city = a.getCity();
        s.state = a.getState();
        s.postalCode = a.getPostalCode();
        s.country = a.getCountry();
        return s;
    }
}