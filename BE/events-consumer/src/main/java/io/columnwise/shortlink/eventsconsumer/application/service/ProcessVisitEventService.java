package io.columnwise.shortlink.eventsconsumer.application.service;

import io.columnwise.shortlink.eventsconsumer.application.port.in.ProcessVisitEventUseCase;
import io.columnwise.shortlink.eventsconsumer.application.port.out.OlapWriterPort;
import io.columnwise.shortlink.eventsconsumer.domain.event.VisitEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 방문 이벤트 처리 서비스(OLAP 적재만 수행하는 최소 구현).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessVisitEventService implements ProcessVisitEventUseCase {

    private final OlapWriterPort olapWriterPort;

    @Override
    public void process(VisitEvent event) {
        // 간단한 로깅 후 OLAP 적재 호출
        log.debug("방문 이벤트 수신: code={}, eventId={}", event.code(), event.eventId());
        olapWriterPort.write(event);
    }
}

