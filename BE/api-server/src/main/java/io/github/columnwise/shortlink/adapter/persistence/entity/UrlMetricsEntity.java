package io.github.columnwise.shortlink.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "url_metrics")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UrlMetricsEntity {
	@Id
	@Column(length = 10, nullable = false)
	private String code;

	@Column(nullable = false)
	private long totalAccesses;
}
