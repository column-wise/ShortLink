package io.columnwise.shortlink.eventsconsumer.adapter.outbound.olap;

import io.columnwise.shortlink.eventsconsumer.application.port.out.OlapWriterPort;
import io.columnwise.shortlink.eventsconsumer.domain.event.VisitEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * ClickHouse OLAP 적재 어댑터(스켈레톤).
 *
 * <p>실제 적재 로직은 이후 구현합니다.</p>
 */
@Slf4j
@Component
public class ClickHouseOlapWriterAdapter implements OlapWriterPort {

    @Override
    public void write(VisitEvent event) {
        // TODO: ClickHouse JDBC/HTTP를 사용해 적재 구현
        log.debug("OLAP 적재 예정: code={}, eventId={}", event.code(), event.eventId());
    }
}

