package io.github.columnwise.shortlink.domain.event;

import java.time.Instant;

/**
 * 스트림으로 발행될 도메인 이벤트의 기본 인터페이스.
 */
public interface DomainEvent {
    /** 안정적인 이벤트 타입(예: "link.hit.v1"). */
    String type();

    /** 파티셔닝 키(예: 링크 코드) — 순서/지역성 보존 목적. */
    String partitionKey();

    /** 이벤트 발생 시각(UTC). */
    Instant occurredAt();
}
