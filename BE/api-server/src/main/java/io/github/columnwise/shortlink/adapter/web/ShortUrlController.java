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
import org.springframework.beans.factory.annotation.Autowired;


/**
 * URL ?¨ì¶• ?œë¹„??REST API ì»´íŠ¸ë¡¤ëŸ¬
 *
 * <p>URL ?¨ì¶• ?œë¹„?¤ì˜ ì£¼ìš” ê¸°ëŠ¥??REST APIë¡??œê³µ?˜ëŠ” ì»´íŠ¸ë¡¤ëŸ¬?…ë‹ˆ??
 * Hexagonal Architecture?ì„œ Web Adapter ??• ???´ë‹¹?˜ë©°, ?¸ë? HTTP ?”ì²­???„ë©”???œë¹„?¤ë¡œ ?„ë‹¬?©ë‹ˆ??</p>
 *
 * <p>?œê³µ ê¸°ëŠ¥:</p>
 * <ul>
 *   <li>URL ?¨ì¶• ?ì„± (POST /api/v1/urls)</li>
 *   <li>URL ë¦¬ë‹¤?´ë ‰??(GET /api/v1/r/{code})</li>
 *   <li>?¼ë³„ ?‘ì† ?µê³„ ì¡°íšŒ (GET /api/v1/urls/{code}/stats)</li>
 * </ul>
 *
 * <p>OpenAPI/Swagger ë¬¸ì„œ?”ë? ì§€?í•˜ë©? Bean Validation???µí•œ ?…ë ¥ ê²€ì¦ì„ ?˜í–‰?©ë‹ˆ??</p>
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Short URL API", description = "URL ?¨ì¶• ?œë¹„??REST API")
public class ShortUrlController {

	private final CreateShortUrlUseCase createShortUrlUseCase;
	private final ResolveUrlUseCase resolveUrlUseCase;
	private final GetStatsUseCase getStatsUseCase;

	@Value("${server.url}")
	private String serverUrl;

	@Autowired
	private HttpServletRequest httpRequest;

	@PostMapping("/urls")
	@Operation(
		summary = "URL ?¨ì¶•",
		description = "ê¸?URL???¨ì¶• ì½”ë“œë¡?ë³€?˜í•©?ˆë‹¤."
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "201",
			description = "URL ?¨ì¶• ?±ê³µ",
			content = @Content(schema = @Schema(implementation = CreateShortUrlResponse.class))
		),
		@ApiResponse(
			responseCode = "400",
			description = "?˜ëª»???”ì²­ (? íš¨?˜ì? ?Šì? URL ?•ì‹)"
		)
	})
	public ResponseEntity<CreateShortUrlResponse> createShortUrl(
		@Parameter(description = "?¨ì¶•??URL ?•ë³´", required = true)
		@Valid @RequestBody CreateShortUrlRequest request
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
		summary = "URL ë¦¬ë‹¤?´ë ‰??,
		description = "?¨ì¶• ì½”ë“œë¥??µí•´ ?ë³¸ URLë¡?ë¦¬ë‹¤?´ë ‰?¸í•©?ˆë‹¤."
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "302",
			description = "?ë³¸ URLë¡?ë¦¬ë‹¤?´ë ‰???±ê³µ"
		),
		@ApiResponse(
			responseCode = "404",
			description = "ì¡´ì¬?˜ì? ?ŠëŠ” ?¨ì¶• ì½”ë“œ"
		)
	})
	public ResponseEntity<Void> redirectToOriginalUrl(
		@Parameter(description = "?¨ì¶• ì½”ë“œ", required = true, example = "abc123")
		@PathVariable("code") String code,
		HttpServletRequest request
	) {
		// ë¡œë“œë°¸ëŸ°???˜ê²½??ê³ ë ¤???´ë¼?´ì–¸???•ë³´ ì¶”ì¶œ
		// todo IP ì£¼ì†Œ ?•ì‹ ê²€ì¦? user-agent ê¸¸ì´ ?œí•œ
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
		summary = "URL ?„ì  ?µê³„ ì¡°íšŒ",
		description = "?¹ì • ?¨ì¶• URL???„ì  ?µê³„ ?•ë³´ë¥?ì¡°íšŒ?©ë‹ˆ?? ì´?ì¡°íšŒ?? ?¤ëŠ˜ ì¡°íšŒ???±ì˜ ?”ì•½ ?•ë³´ë¥??œê³µ?©ë‹ˆ??"
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "?µê³„ ì¡°íšŒ ?±ê³µ",
			content = @Content(schema = @Schema(implementation = UrlMetrics.class))
		),
		@ApiResponse(
			responseCode = "404",
			description = "ì¡´ì¬?˜ì? ?ŠëŠ” ?¨ì¶• ì½”ë“œ"
		)
	})
	public ResponseEntity<UrlMetrics> getUrlMetrics(
		@Parameter(description = "?¨ì¶• ì½”ë“œ", required = true, example = "abc123")
		@PathVariable("code") String code
	) {
		UrlMetrics metrics = getStatsUseCase.getUrlMetrics(code);
		return ResponseEntity.ok(metrics);
	}
}
