package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.customer.dto;

import com.fedebacelar.bank.onboarding.domain.model.ApplicantData;
import java.time.LocalDate;
import java.util.List;

public record RegisterCustomerRequest(
        String firstName, String middleName, String lastName, LocalDate birthDate, String nationality,
        String documentType, String documentNumber, String issuingCountry, LocalDate documentExpirationDate,
        List<ContactRequest> contactPoints, List<AddressRequest> addresses
) {
    public static RegisterCustomerRequest from(String email, ApplicantData data) {
        return new RegisterCustomerRequest(data.firstName(), data.middleName(), data.lastName(), data.birthDate(),
                data.nationality(), data.documentType().name(), data.documentNumber(), data.documentIssuingCountry(),
                data.documentExpirationDate(),
                List.of(new ContactRequest("EMAIL", email, true), new ContactRequest("PHONE", data.phoneNumber(), false)),
                List.of(new AddressRequest("HOME", data.street(), data.streetNumber(), data.city(), data.province(), data.postalCode(), data.country())));
    }

    public record ContactRequest(String type, String value, boolean verified) {}
    public record AddressRequest(String type, String street, String streetNumber, String city, String province, String postalCode, String country) {}
}
