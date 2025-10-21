package io.columnwise.shortlink.eventsconsumer.adapter.inbound.kafka;

import io.columnwise.shortlink.eventsconsumer.application.port.in.ProcessVisitEventUseCase;
import io.columnwise.shortlink.eventsconsumer.domain.event.VisitEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka 리스너(입력 어댑터): 방문 이벤트 수신.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaVisitEventListener {

    private final ProcessVisitEventUseCase useCase;

    @KafkaListener(topics = "${app.stream.input.linkHits:link_hits}", groupId = "${spring.kafka.consumer.group-id:shortlink-events-consumer}")
    public void onMessage(VisitEvent event) {
        log.debug("Kafka 이벤트 수신: code={}, eventId={}", event.code(), event.eventId());
        useCase.process(event);
    }
}

