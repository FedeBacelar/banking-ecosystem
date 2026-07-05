package com.fedebacelar.bank.onboarding;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OnboardingServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
