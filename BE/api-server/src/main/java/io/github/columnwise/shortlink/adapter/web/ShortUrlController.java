package io.github.columnwise.shortlink.adapter.web;

import io.github.columnwise.shortlink.adapter.web.dto.CreateShortUrlRequest;
import io.github.columnwise.shortlink.adapter.web.dto.CreateShortUrlResponse;
import io.github.columnwise.shortlink.application.port.in.CreateShortUrlUseCase;
import io.github.columnwise.shortlink.application.port.in.GetStatsUseCase;
import io.github.columnwise.shortlink.application.port.in.ResolveUrlUseCase;
import io.github.columnwise.shortlink.domain.model.ShortUrl;
import io.github.columnwise.shortlink.domain.model.UrlMetrics;
import io.github.columnwise.shortlink.util.ClientInfoExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;


/**
 * URL ?�축 ?�비??REST API 컴트롤러
 *
 * <p>URL ?�축 ?�비?�의 주요 기능??REST API�??�공?�는 컴트롤러?�니??
 * Hexagonal Architecture?�서 Web Adapter ??��???�당?�며, ?��? HTTP ?�청???�메???�비?�로 ?�달?�니??</p>
 *
 * <p>?�공 기능:</p>
 * <ul>
 *   <li>URL ?�축 ?�성 (POST /api/v1/urls)</li>
 *   <li>URL 리다?�렉??(GET /api/v1/r/{code})</li>
 *   <li>?�별 ?�속 ?�계 조회 (GET /api/v1/urls/{code}/stats)</li>
 * </ul>
 *
 * <p>OpenAPI/Swagger 문서?��? 지?�하�? Bean Validation???�한 ?�력 검증을 ?�행?�니??</p>
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Short URL API", description = "URL ?�축 ?�비??REST API")
public class ShortUrlController {

	private final CreateShortUrlUseCase createShortUrlUseCase;
	private final ResolveUrlUseCase resolveUrlUseCase;
	private final GetStatsUseCase getStatsUseCase;

	@Value("${server.url}")
	private String serverUrl;

    

	@PostMapping("/urls")
	@Operation(
		summary = "URL ?�축",
		description = "�?URL???�축 코드�?변?�합?�다."
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "201",
			description = "URL ?�축 ?�공",
			content = @Content(schema = @Schema(implementation = CreateShortUrlResponse.class))
		),
		@ApiResponse(
			responseCode = "400",
			description = "?�못???�청 (?�효?��? ?��? URL ?�식)"
		)
	})
    public ResponseEntity<CreateShortUrlResponse> createShortUrl(
        @Parameter(description = "축소할 URL 정보", required = true)
        @Valid @RequestBody CreateShortUrlRequest request,
        HttpServletRequest httpRequest
    ) {
		ShortUrl shortUrl = createShortUrlUseCase.createShortUrl(request.longUrl());


		String scheme = httpRequest.getHeader("X-Forwarded-Proto");
		if (scheme == null || scheme.isBlank()) {
			scheme = httpRequest.getScheme();
		}

		String host = httpRequest.getHeader("X-Forwarded-Host");
		if (host == null || host.isBlank()) {
			host = httpRequest.getHeader("Host");
		}
		if (host == null || host.isBlank()) {
			String serverName = httpRequest.getServerName();
			int port = httpRequest.getServerPort();
			boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
					|| ("https".equalsIgnoreCase(scheme) && port == 443);
			host = defaultPort ? serverName : serverName + ":" + port;
		}
		// If multiple hosts forwarded, take the first
		int commaIdx = host.indexOf(',');
		if (commaIdx > 0) {
			host = host.substring(0, commaIdx).trim();
		}
		// Append forwarded port if provided and non-default and host has no explicit port
		if (!host.contains(":")) {
			String fwdPort = httpRequest.getHeader("X-Forwarded-Port");
			if (fwdPort != null && !fwdPort.isBlank()) {
				try {
					int p = Integer.parseInt(fwdPort.trim());
					boolean defaultPort = ("http".equalsIgnoreCase(scheme) && p == 80)
							|| ("https".equalsIgnoreCase(scheme) && p == 443);
					if (!defaultPort) {
						host = host + ":" + p;
					}
				} catch (NumberFormatException ignored) {}
			}
		}

		String baseUrl = scheme + "://" + host;

		CreateShortUrlResponse response = new CreateShortUrlResponse(
				shortUrl.code(),
				baseUrl + "/api/v1/r/" + shortUrl.code()
		);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

    @GetMapping("/r/{code}")
    @Operation(
        summary = "URL 리다이렉트",
        description = "단축 코드로 원본 URL로 리다이렉트합니다."
    )
	@ApiResponses({
		@ApiResponse(
			responseCode = "302",
			description = "?�본 URL�?리다?�렉???�공"
		),
		@ApiResponse(
			responseCode = "404",
			description = "존재?��? ?�는 ?�축 코드"
		)
	})
	public ResponseEntity<Void> redirectToOriginalUrl(
		@Parameter(description = "?�축 코드", required = true, example = "abc123")
		@PathVariable("code") String code,
		HttpServletRequest request
	) {
		// 로드밸런???�경??고려???�라?�언???�보 추출
		// todo IP 주소 ?�식 검�? user-agent 길이 ?�한
		String clientIp = ClientInfoExtractor.getClientIp(request);
		String userAgent = request.getHeader("User-Agent");
		String browserFamily = ClientInfoExtractor.extractBrowserFamily(userAgent);
		String deviceType = ClientInfoExtractor.extractDeviceType(userAgent);

		String longUrl = resolveUrlUseCase.resolveUrl(code, clientIp, browserFamily, deviceType);

		return ResponseEntity.status(HttpStatus.FOUND)
				.header(HttpHeaders.LOCATION, longUrl)
				.build();
	}

	@GetMapping("/urls/{code}/metrics")
	@Operation(
		summary = "URL ?�적 ?�계 조회",
		description = "?�정 ?�축 URL???�적 ?�계 ?�보�?조회?�니?? �?조회?? ?�늘 조회???�의 ?�약 ?�보�??�공?�니??"
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "?�계 조회 ?�공",
			content = @Content(schema = @Schema(implementation = UrlMetrics.class))
		),
		@ApiResponse(
			responseCode = "404",
			description = "존재?��? ?�는 ?�축 코드"
		)
	})
	public ResponseEntity<UrlMetrics> getUrlMetrics(
		@Parameter(description = "?�축 코드", required = true, example = "abc123")
		@PathVariable("code") String code
	) {
		UrlMetrics metrics = getStatsUseCase.getUrlMetrics(code);
		return ResponseEntity.ok(metrics);
	}
}

