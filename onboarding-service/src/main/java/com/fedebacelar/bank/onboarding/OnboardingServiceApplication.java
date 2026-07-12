package com.fedebacelar.bank.onboarding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.fedebacelar.bank.onboarding.infrastructure.config.OnboardingReviewProperties;
import com.fedebacelar.bank.onboarding.infrastructure.config.OnboardingProvisioningProperties;
import com.fedebacelar.bank.onboarding.infrastructure.config.OnboardingNotificationProperties;

@EnableFeignClients
@EnableScheduling
@EnableConfigurationProperties({
        OnboardingReviewProperties.class,
        OnboardingProvisioningProperties.class,
        OnboardingNotificationProperties.class
})
@SpringBootApplication
public class OnboardingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnboardingServiceApplication.class, args);
    }
}
