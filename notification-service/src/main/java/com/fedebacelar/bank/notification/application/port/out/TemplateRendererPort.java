package com.fedebacelar.bank.notification.application.port.out;

import com.fedebacelar.bank.notification.domain.enums.NotificationTemplateCode;
import com.fedebacelar.bank.notification.domain.model.RenderedNotification;
import java.util.Map;

public interface TemplateRendererPort {

    RenderedNotification render(NotificationTemplateCode templateCode, Map<String, String> variables);
}

