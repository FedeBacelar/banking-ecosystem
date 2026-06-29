package com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fedebacelar.bank.customer.domain.enums.CustomerStatus;
import com.fedebacelar.bank.customer.domain.enums.DocumentType;
import com.fedebacelar.bank.customer.domain.enums.KycStatus;
import com.fedebacelar.bank.customer.domain.enums.PartyLifecycleStatus;
import com.fedebacelar.bank.customer.domain.enums.PartyType;
import com.fedebacelar.bank.customer.domain.enums.RiskLevel;
import com.fedebacelar.bank.customer.domain.model.Customer;
import com.fedebacelar.bank.customer.domain.model.CustomerStatusHistory;
import com.fedebacelar.bank.customer.domain.model.IdentificationDocument;
import com.fedebacelar.bank.customer.domain.model.KycProfile;
import com.fedebacelar.bank.customer.domain.model.NaturalPerson;
import com.fedebacelar.bank.customer.domain.model.NaturalPersonCustomer;
import com.fedebacelar.bank.customer.domain.model.Party;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class CustomerPersistenceAdapterIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    private CustomerPersistenceAdapter adapter;

    @Autowired
    private CustomerNumberGeneratorAdapter customerNumberGeneratorAdapter;

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-06-13T00:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Test
    void savesAndFindsCustomerByDocument() {
        NaturalPersonCustomer customer = aggregate();

        adapter.save(customer);

        assertThat(adapter.existsDocument(DocumentType.DNI, "30111222", "AR")).isTrue();
        var found = adapter.findByDocument(DocumentType.DNI, "30111222", "AR");

        assertThat(found).isPresent();
        assertThat(found.get().customer().status()).isEqualTo(CustomerStatus.PENDING_KYC);
        assertThat(found.get().kycProfile().status()).isEqualTo(KycStatus.PENDING_REVIEW);

        assertThat(adapter.findByCustomerNumber("CUS-2026-000001")).isPresent();
        assertThat(adapter.findStatusHistory(customer.customer().id())).hasSize(1);
    }

    @Test
    void generatesCustomerNumbersFromTransactionalSequence() {
        assertThat(customerNumberGeneratorAdapter.nextCustomerNumber()).isEqualTo("CUS-2026-000001");
        assertThat(customerNumberGeneratorAdapter.nextCustomerNumber()).isEqualTo("CUS-2026-000002");
    }

    private NaturalPersonCustomer aggregate() {
        Instant now = Instant.now();
        UUID partyId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        return new NaturalPersonCustomer(
                new Party(partyId, PartyType.NATURAL_PERSON, PartyLifecycleStatus.REGISTERED, now, now),
                new NaturalPerson(partyId, "Federico", null, "Bacelar", LocalDate.of(1990, 1, 15), "AR"),
                new Customer(customerId, partyId, "CUS-2026-000001", CustomerStatus.PENDING_KYC, LocalDate.now(), null, now, now, null),
                new IdentificationDocument(UUID.randomUUID(), partyId, DocumentType.DNI, "30111222", "AR", null, true),
                List.of(),
                List.of(),
                new KycProfile(UUID.randomUUID(), customerId, RiskLevel.LOW, KycStatus.PENDING_REVIEW, null, null),
                List.of(new CustomerStatusHistory(UUID.randomUUID(), customerId, null, CustomerStatus.PENDING_KYC, "created", now))
        );
    }
}
