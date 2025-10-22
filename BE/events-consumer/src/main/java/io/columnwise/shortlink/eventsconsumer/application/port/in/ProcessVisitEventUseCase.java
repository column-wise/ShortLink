package io.columnwise.shortlink.eventsconsumer.application.port.in;

import io.columnwise.shortlink.eventsconsumer.domain.event.VisitEvent;

/**
 * 방문 이벤트 처리 유스케이스(도메인 로직 진입점).
 */
public interface ProcessVisitEventUseCase {
    void process(VisitEvent event);
}

