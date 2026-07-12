package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.document;

import static org.assertj.core.api.Assertions.assertThat;

import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.document.dto.DocumentMetadataResponse;
import com.sun.net.httpserver.HttpServer;
import feign.Feign;
import feign.form.spring.SpringFormEncoder;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.mock.web.MockMultipartFile;

class DocumentFeignClientMultipartContractTest {

    private final AtomicReference<byte[]> requestBody = new AtomicReference<>();
    private final AtomicReference<String> requestQuery = new AtomicReference<>();
    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/internal/documents", exchange -> {
            requestQuery.set(exchange.getRequestURI().getRawQuery());
            requestBody.set(exchange.getRequestBody().readAllBytes());
            byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void encodesTheFileAsTheRequiredMultipartPart() {
        DocumentFeignClient client = Feign.builder()
                .contract(new SpringMvcContract())
                .encoder(new SpringFormEncoder())
                .decoder((response, type) -> new DocumentMetadataResponse(
                        UUID.randomUUID(), "ONBOARDING_APPLICATION", "application", "DNI_FRONT", "STORED"
                ))
                .target(DocumentFeignClient.class, "http://localhost:" + server.getAddress().getPort());
        MockMultipartFile file = new MockMultipartFile(
                "file", "dni-front.png", "image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}
        );

        client.upload(
                "onboarding:application:DNI_FRONT:hash",
                "a".repeat(64),
                "ONBOARDING_APPLICATION",
                "application",
                "DNI_FRONT",
                file
        );

        String multipartBody = new String(requestBody.get(), StandardCharsets.ISO_8859_1);
        assertThat(requestQuery.get()).contains(
                "businessContext=ONBOARDING_APPLICATION",
                "businessReferenceId=application",
                "category=DNI_FRONT"
        );
        assertThat(multipartBody).contains(
                "name=\"file\"",
                "filename=\"dni-front.png\"",
                "Content-Type: image/png"
        );
    }
}
