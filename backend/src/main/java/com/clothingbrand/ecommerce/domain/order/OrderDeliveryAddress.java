package com.clothingbrand.ecommerce.domain.order;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "order_delivery_addresses")
public class OrderDeliveryAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private CustomerOrder order;

    @Column(name = "recipient_name", nullable = false, length = 200, updatable = false)
    private String recipientName;

    @Column(name = "phone_number", nullable = false, length = 32, updatable = false)
    private String phoneNumber;

    @Column(name = "address_line_1", nullable = false, length = 255, updatable = false)
    private String addressLine1;

    @Column(name = "address_line_2", length = 255, updatable = false)
    private String addressLine2;

    @Column(nullable = false, length = 120, updatable = false)
    private String city;

    @Column(length = 120, updatable = false)
    private String region;

    @Column(name = "postal_code", length = 32, updatable = false)
    private String postalCode;

    @Column(nullable = false, length = 120, updatable = false)
    private String country;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected OrderDeliveryAddress() {
    }

    public OrderDeliveryAddress(CustomerOrder order, String recipientName, String phoneNumber,
                                String addressLine1, String addressLine2, String city,
                                String region, String postalCode, String country) {
        this.order = order;
        this.recipientName = recipientName;
        this.phoneNumber = phoneNumber;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.region = region;
        this.postalCode = postalCode;
        this.country = country;
    }

    public Long getId() { return id; }
    public CustomerOrder getOrder() { return order; }
    public String getRecipientName() { return recipientName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getAddressLine1() { return addressLine1; }
    public String getAddressLine2() { return addressLine2; }
    public String getCity() { return city; }
    public String getRegion() { return region; }
    public String getPostalCode() { return postalCode; }
    public String getCountry() { return country; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
