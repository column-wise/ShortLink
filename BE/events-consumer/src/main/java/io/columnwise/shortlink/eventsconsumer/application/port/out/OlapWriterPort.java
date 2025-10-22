package io.columnwise.shortlink.eventsconsumer.application.port.out;

import io.columnwise.shortlink.eventsconsumer.domain.event.VisitEvent;

/**
 * OLAP 적재 포트(ClickHouse/Pinot/Druid 등 구현 교체 가능).
 */
public interface OlapWriterPort {
    void write(VisitEvent event);
}

