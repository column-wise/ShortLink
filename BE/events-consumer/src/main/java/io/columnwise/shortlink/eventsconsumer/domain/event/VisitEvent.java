package io.columnwise.shortlink.eventsconsumer.domain.event;

import java.time.Instant;

/**
 * 단축 링크 방문 이벤트(OLAP 적재용 최소 스키마).
 */
public record VisitEvent(
        String eventId,
        String code,
        String visitorHash,
        String uaFamily,
        String deviceType,
        String referer,
        Instant occurredAt
) {}

