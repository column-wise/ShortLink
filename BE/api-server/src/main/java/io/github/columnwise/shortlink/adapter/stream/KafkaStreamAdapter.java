package io.github.columnwise.shortlink.adapter.stream;

import io.github.columnwise.shortlink.application.port.out.StreamPort;
import io.github.columnwise.shortlink.domain.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka 기반 스트림 어댑터 구현체.
 *
 * <p>도메인 이벤트를 JSON으로 직렬화하여 Kafka로 발행합니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaStreamAdapter implements StreamPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.linkHits:link_hits}")
    private String linkHitsTopic;

    @Override
    public void publish(DomainEvent event) {
        try {
            String key = event.partitionKey();
            kafkaTemplate.send(linkHitsTopic, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Kafka publish failed. topic={}, key={}, type={}", linkHitsTopic, key, event.type(), ex);
                        } else if (result != null && result.getRecordMetadata() != null) {
                            log.debug("Kafka published. topic={}, partition={}, offset={}",
                                    result.getRecordMetadata().topic(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception e) {
            log.warn("Kafka publish threw exception. type={}, key={}", event.type(), event.partitionKey(), e);
        }
    }
}
