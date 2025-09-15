package io.github.columnwise.shortlink.adapter.web;

import io.github.columnwise.shortlink.adapter.web.dto.CreateShortUrlRequest;
import io.github.columnwise.shortlink.adapter.web.dto.CreateShortUrlResponse;
import io.github.columnwise.shortlink.application.port.in.CreateShortUrlUseCase;
import io.github.columnwise.shortlink.application.port.in.GetStatsUseCase;
import io.github.columnwise.shortlink.application.port.in.ResolveUrlUseCase;
import io.github.columnwise.shortlink.domain.model.ShortUrl;
import io.github.columnwise.shortlink.domain.model.DailyStatistics;
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
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDate;
import java.util.List;

/**
 * URL 단축 서비스 REST API 컴트롤러
 *
 * <p>URL 단축 서비스의 주요 기능을 REST API로 제공하는 컴트롤러입니다.
 * Hexagonal Architecture에서 Web Adapter 역할을 담당하며, 외부 HTTP 요청을 도메인 서비스로 전달합니다.</p>
 *
 * <p>제공 기능:</p>
 * <ul>
 *   <li>URL 단축 생성 (POST /api/v1/urls)</li>
 *   <li>URL 리다이렉트 (GET /api/v1/r/{code})</li>
 *   <li>일별 접속 통계 조회 (GET /api/v1/urls/{code}/stats)</li>
 * </ul>
 *
 * <p>OpenAPI/Swagger 문서화를 지원하며, Bean Validation을 통한 입력 검증을 수행합니다.</p>
 */
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
		summary = "URL 일별 접속 통계 조회",
		description = "특정 단축 URL의 일별 접속 통계 목록을 조회합니다. 프론트엔드에서 시간대별, 요일별 분석이 가능합니다."
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "통계 조회 성공",
			content = @Content(array = @ArraySchema(schema = @Schema(implementation = DailyStatistics.class)))
		),
		@ApiResponse(
			responseCode = "400",
			description = "잘못된 요청 (잘못된 날짜 형식 또는 날짜 범위 오류)"
		),
		@ApiResponse(
			responseCode = "404",
			description = "존재하지 않는 단축 코드"
		)
	})
	public ResponseEntity<List<DailyStatistics>> getDailyStatistics(
		@Parameter(description = "단축 코드", required = true, example = "abc123")
		@PathVariable("code") String code,
		
		@Parameter(description = "시작 날짜 (YYYY-MM-DD, 생략시 30일 전)", example = "2024-01-01")
		@RequestParam(required = false) 
		@org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
		LocalDate startDate,
		
		@Parameter(description = "종료 날짜 (YYYY-MM-DD, 생략시 오늘)", example = "2024-01-31")
		@RequestParam(required = false)
		@org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
		LocalDate endDate
	) {
		// 기본값 처리와 검증
		if (startDate == null) {
			startDate = LocalDate.now().minusDays(30);
		}
		if (endDate == null) {
			endDate = LocalDate.now();
		}
		
		// 날짜 범위 검증: 시작일이 종료일보다 늦으면 안됨
		if (startDate.isAfter(endDate)) {
			throw new IllegalArgumentException("Start date cannot be after end date");
		}
		
		List<DailyStatistics> statistics = getStatsUseCase.getDailyStatistics(code, startDate, endDate);
		return ResponseEntity.ok(statistics);
	}
}
