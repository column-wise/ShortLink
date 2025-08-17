package io.github.columnwise.shortlink.adapter.web;

import io.github.columnwise.shortlink.adapter.web.dto.CreateShortUrlRequest;
import io.github.columnwise.shortlink.adapter.web.dto.CreateShortUrlResponse;
import io.github.columnwise.shortlink.application.port.in.CreateShortUrlUseCase;
import io.github.columnwise.shortlink.application.port.in.GetStatsUseCase;
import io.github.columnwise.shortlink.application.port.in.ResolveUrlUseCase;
import io.github.columnwise.shortlink.domain.model.ShortUrl;
import io.github.columnwise.shortlink.domain.model.UrlAccessLog;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
public class ShortUrlController {

	private final CreateShortUrlUseCase createShortUrlUseCase;
	private final ResolveUrlUseCase resolveUrlUseCase;
	private final GetStatsUseCase getStatsUseCase;

	@PostMapping("/urls")
	public ResponseEntity<CreateShortUrlResponse> createShortUrl(@Valid @RequestBody CreateShortUrlRequest request) {
		ShortUrl shortUrl = createShortUrlUseCase.createShortUrl(request.longUrl());
		
		CreateShortUrlResponse response = new CreateShortUrlResponse(
				shortUrl.code(),
				"http://localhost:8080/api/v1/r/" + shortUrl.code()
		);
		
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/r/{code}")
	public RedirectView redirectToOriginalUrl(@PathVariable("code") String code) {
		String longUrl = resolveUrlUseCase.resolveUrl(code);
		return new RedirectView(longUrl);
	}

	@GetMapping("/urls/{code}/stats")
	public ResponseEntity<List<UrlAccessLog>> getAccessLogs(@PathVariable("code") String code) {
		List<UrlAccessLog> accessLogs = getStatsUseCase.getAccessLogs(code);
		return ResponseEntity.ok(accessLogs);
	}
}
