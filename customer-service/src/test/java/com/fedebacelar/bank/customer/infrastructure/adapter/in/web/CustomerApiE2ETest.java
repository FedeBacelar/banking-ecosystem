package com.fedebacelar.bank.customer.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class CustomerApiE2ETest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void completesNaturalPersonLifecycle() throws Exception {
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();

        JsonNode created = post(client, "/customers/natural-persons", """
                {
                  "firstName": "Federico",
                  "lastName": "Bacelar",
                  "birthDate": "1990-01-15",
                  "nationality": "AR",
                  "documentType": "DNI",
                  "documentNumber": "E2E-30111222",
                  "issuingCountry": "AR"
                }
                """);
        UUID customerId = UUID.fromString(created.get("customerId").asText());
        String customerNumber = created.get("customerNumber").asText();

        assertThat(created.get("status").asText()).isEqualTo("PENDING_KYC");
        assertThat(get(client, "/customers/" + customerId).get("customerNumber").asText()).isEqualTo(customerNumber);
        assertThat(get(client, "/customers/by-document?type=DNI&number=E2E-30111222&country=AR").get("customerId").asText()).isEqualTo(customerId.toString());
        assertThat(get(client, "/customers/by-number/" + customerNumber).get("customerId").asText()).isEqualTo(customerId.toString());

        JsonNode active = patch(client, "/customers/" + customerId + "/kyc/approve", null);
        assertThat(active.get("status").asText()).isEqualTo("ACTIVE");
        assertThat(active.get("kycStatus").asText()).isEqualTo("APPROVED");

        assertThat(patch(client, "/customers/" + customerId + "/suspend", "{\"reason\":\"risk alert\"}").get("status").asText()).isEqualTo("SUSPENDED");
        assertThat(patch(client, "/customers/" + customerId + "/reactivate", "{\"reason\":\"manual review\"}").get("status").asText()).isEqualTo("ACTIVE");
        assertThat(patch(client, "/customers/" + customerId + "/close", "{\"reason\":\"customer request\"}").get("status").asText()).isEqualTo("CLOSED");

        JsonNode history = get(client, "/customers/" + customerId + "/status-history");
        assertThat(history).hasSize(5);
        assertThat(history.get(4).get("newStatus").asText()).isEqualTo("CLOSED");
    }

    private JsonNode get(RestClient client, String path) throws Exception {
        return objectMapper.readTree(client.get().uri(path).retrieve().body(String.class));
    }

    private JsonNode post(RestClient client, String path, String body) throws Exception {
        return objectMapper.readTree(client.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class));
    }

    private JsonNode patch(RestClient client, String path, String body) throws Exception {
        var request = client.patch().uri(path);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).body(body);
        }
        return objectMapper.readTree(request.retrieve().body(String.class));
    }
}
