package com.fedebacelar.bank.notification.infrastructure.adapter.out.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fedebacelar.bank.notification.domain.enums.NotificationTemplateCode;
import com.fedebacelar.bank.notification.domain.exception.MissingTemplateVariableException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InMemoryTemplateRendererAdapterTest {

    private final InMemoryTemplateRendererAdapter renderer = new InMemoryTemplateRendererAdapter();

    @Test
    void rendersMagicLinkTemplate() {
        var rendered = renderer.render(NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK, Map.of(
                "magicLink", "http://localhost:4200/onboarding/continue?token=abc",
                "expiresInMinutes", "30"
        ));

        assertThat(rendered.subject()).contains("Nerva Banking");
        assertThat(rendered.body()).contains("http://localhost:4200/onboarding/continue?token=abc");
        assertThat(rendered.body()).contains("30");
    }

    @Test
    void rejectsMissingVariables() {
        assertThatThrownBy(() -> renderer.render(NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK, Map.of(
                "magicLink", "http://localhost"
        )))
                .isInstanceOf(MissingTemplateVariableException.class)
                .hasMessageContaining("expiresInMinutes");
    }
}

