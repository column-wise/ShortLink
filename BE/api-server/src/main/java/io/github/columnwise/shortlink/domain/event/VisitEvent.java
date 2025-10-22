package io.github.columnwise.shortlink.domain.event;

import java.time.Instant;
import java.util.Objects;

/**
 * 단축 링크 리다이렉트 발생 시 전송되는 방문 이벤트.
 */
public record VisitEvent(
        String eventId,
        String code,
        String visitorHash,
        String uaFamily,
        String deviceType,
        String referer,
        Instant occurredAt
) implements DomainEvent {

    /** 이벤트 타입 */
    public static final String TYPE = "link.hit.v1";

    public VisitEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(occurredAt, "occurredAt");
        if (eventId.isBlank()) throw new IllegalArgumentException("eventId는 공백일 수 없습니다");
        if (code.isBlank()) throw new IllegalArgumentException("code는 공백일 수 없습니다");
        if (uaFamily == null || uaFamily.isBlank()) uaFamily = "unknown";
        if (deviceType == null || deviceType.isBlank()) deviceType = "unknown";
    }

    @Override
    public String type() { return TYPE; }

    @Override
    public String partitionKey() { return code; }
}
