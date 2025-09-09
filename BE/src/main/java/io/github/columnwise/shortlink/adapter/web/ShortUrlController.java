package io.github.columnwise.shortlink.adapter.web;

import io.github.columnwise.shortlink.adapter.web.dto.CreateShortUrlRequest;
import io.github.columnwise.shortlink.adapter.web.dto.CreateShortUrlResponse;
import io.github.columnwise.shortlink.application.port.in.CreateShortUrlUseCase;
import io.github.columnwise.shortlink.application.port.in.GetStatsUseCase;
import io.github.columnwise.shortlink.application.port.in.ResolveUrlUseCase;
import io.github.columnwise.shortlink.domain.model.ShortUrl;
import io.github.columnwise.shortlink.domain.model.UrlAccessLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Short URL API", description = "URL 단축 서비스 REST API")
public class ShortUrlController {

	private final CreateShortUrlUseCase createShortUrlUseCase;
	private final ResolveUrlUseCase resolveUrlUseCase;
	private final GetStatsUseCase getStatsUseCase;
	
	@Value("${server.url}")
	private String serverUrl;

	@PostMapping("/urls")
	@Operation(
		summary = "URL 단축",
		description = "긴 URL을 단축 코드로 변환합니다."
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "201",
			description = "URL 단축 성공",
			content = @Content(schema = @Schema(implementation = CreateShortUrlResponse.class))
		),
		@ApiResponse(
			responseCode = "400",
			description = "잘못된 요청 (유효하지 않은 URL 형식)"
		)
	})
	public ResponseEntity<CreateShortUrlResponse> createShortUrl(
		@Parameter(description = "단축할 URL 정보", required = true)
		@Valid @RequestBody CreateShortUrlRequest request
	) {
		ShortUrl shortUrl = createShortUrlUseCase.createShortUrl(request.longUrl());
		
		CreateShortUrlResponse response = new CreateShortUrlResponse(
				shortUrl.code(),
				serverUrl + "/api/v1/r/" + shortUrl.code()
		);
		
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/r/{code}")
	@Operation(
		summary = "URL 리다이렉트",
		description = "단축 코드를 통해 원본 URL로 리다이렉트합니다."
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "302",
			description = "원본 URL로 리다이렉트 성공"
		),
		@ApiResponse(
			responseCode = "404",
			description = "존재하지 않는 단축 코드"
		)
	})
	public RedirectView redirectToOriginalUrl(
		@Parameter(description = "단축 코드", required = true, example = "abc123")
		@PathVariable("code") String code
	) {
		String longUrl = resolveUrlUseCase.resolveUrl(code);
		return new RedirectView(longUrl);
	}

	@GetMapping("/urls/{code}/stats")
	@Operation(
		summary = "URL 접속 통계 조회",
		description = "특정 단축 URL의 접속 로그 및 통계 정보를 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "통계 조회 성공",
			content = @Content(schema = @Schema(implementation = UrlAccessLog.class))
		),
		@ApiResponse(
			responseCode = "404",
			description = "존재하지 않는 단축 코드"
		)
	})
	public ResponseEntity<List<UrlAccessLog>> getAccessLogs(
		@Parameter(description = "단축 코드", required = true, example = "abc123")
		@PathVariable("code") String code
	) {
		List<UrlAccessLog> accessLogs = getStatsUseCase.getAccessLogs(code);
		return ResponseEntity.ok(accessLogs);
	}
}
