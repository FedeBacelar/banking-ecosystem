package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.mapper;

import com.fedebacelar.bank.onboarding.domain.model.ApplicantData;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.entity.OnboardingApplicantDataEntity;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OnboardingApplicantDataPersistenceMapper {

    public OnboardingApplicantDataEntity toEntity(ApplicantData applicantData) {
        OnboardingApplicantDataEntity entity = new OnboardingApplicantDataEntity();
        entity.setApplicationId(applicantData.applicationId().toString());
        entity.setFirstName(applicantData.firstName());
        entity.setMiddleName(applicantData.middleName());
        entity.setLastName(applicantData.lastName());
        entity.setBirthDate(applicantData.birthDate());
        entity.setNationality(applicantData.nationality());
        entity.setDocumentType(applicantData.documentType());
        entity.setDocumentNumber(applicantData.documentNumber());
        entity.setDocumentIssuingCountry(applicantData.documentIssuingCountry());
        entity.setDocumentExpirationDate(applicantData.documentExpirationDate());
        entity.setPhoneNumber(applicantData.phoneNumber());
        entity.setStreet(applicantData.street());
        entity.setStreetNumber(applicantData.streetNumber());
        entity.setCity(applicantData.city());
        entity.setProvince(applicantData.province());
        entity.setPostalCode(applicantData.postalCode());
        entity.setCountry(applicantData.country());
        entity.setCreatedAt(applicantData.createdAt());
        entity.setUpdatedAt(applicantData.updatedAt());
        entity.setVersion(applicantData.version());
        return entity;
    }

    public ApplicantData toDomain(OnboardingApplicantDataEntity entity) {
        return new ApplicantData(
                UUID.fromString(entity.getApplicationId()),
                entity.getFirstName(),
                entity.getMiddleName(),
                entity.getLastName(),
                entity.getBirthDate(),
                entity.getNationality(),
                entity.getDocumentType(),
                entity.getDocumentNumber(),
                entity.getDocumentIssuingCountry(),
                entity.getDocumentExpirationDate(),
                entity.getPhoneNumber(),
                entity.getStreet(),
                entity.getStreetNumber(),
                entity.getCity(),
                entity.getProvince(),
                entity.getPostalCode(),
                entity.getCountry(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }
}
