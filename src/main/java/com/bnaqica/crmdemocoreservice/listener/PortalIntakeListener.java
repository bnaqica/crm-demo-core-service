package com.bnaqica.crmdemocoreservice.listener;

import com.bnaqica.crmdemocoreservice.service.SubmissionIntakeService;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/** Hands each message on {@code portal-intake}, with its {@code type} header, to {@link SubmissionIntakeService}. */
@Component
@RequiredArgsConstructor
public class PortalIntakeListener {

    private final SubmissionIntakeService intake;

    @KafkaListener(topics = "${crm.topics.portal-intake}")
    void on(String payload, @Header(name = "type", required = false) byte[] type) {
        intake.handle(type == null ? null : new String(type, StandardCharsets.UTF_8), payload);
    }
}
