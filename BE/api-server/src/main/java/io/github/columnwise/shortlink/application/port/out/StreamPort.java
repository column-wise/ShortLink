package io.github.columnwise.shortlink.application.port.out;

import io.github.columnwise.shortlink.domain.event.DomainEvent;

import java.util.Collection;

/**
 * 도메인 이벤트를 스트림으로 발행하기 위한 아웃바운드 포트.
 *
 * <p>Kafka, Redis Streams 등 구체 기술에 의존하지 않는 추상화입니다.
 * 구현체가 직렬화·라우팅을 담당하며, 애플리케이션 코드는 이 포트에만 의존합니다.</p>
 */
public interface StreamPort {

    /**
     * 단일 도메인 이벤트를 발행합니다.
     */
    void publish(DomainEvent event);

    /**
     * 여러 도메인 이벤트를 일괄 발행합니다.
     */
    default void publishAll(Collection<? extends DomainEvent> events) {
        if (events == null || events.isEmpty()) return;
        for (DomainEvent e : events) publish(e);
    }
}
