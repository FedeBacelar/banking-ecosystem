package com.fedebacelar.bank.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = {
        "notification.email.from=no-reply@nerva.local",
        "notification.action-links.onboarding-allowed-origins=http://localhost:4200",
        "notification.action-links.keycloak-allowed-origins=http://localhost:8090",
        "spring.mail.host=localhost",
        "spring.mail.port=2525"
})
@Import(TestcontainersConfiguration.class)
class NotificationServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
