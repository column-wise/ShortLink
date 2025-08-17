package io.github.columnwise.shortlink.application.port.in;

import io.github.columnwise.shortlink.domain.model.UrlAccessLog;
import java.util.List;

public interface GetStatsUseCase {
    List<UrlAccessLog> getAccessLogs(String code);
}
