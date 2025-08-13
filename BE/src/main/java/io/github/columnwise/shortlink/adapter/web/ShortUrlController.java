package io.github.columnwise.shortlink.adapter.web;

import io.github.columnwise.shortlink.adapter.web.dto.CreateShortUrlRequest;
import io.github.columnwise.shortlink.adapter.web.dto.CreateShortUrlResponse;
import io.github.columnwise.shortlink.domain.model.UrlAccessLog;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ShortUrlController {

	@PostMapping("/urls")
	public ResponseEntity<CreateShortUrlResponse> createShortUrl(CreateShortUrlRequest request) {
		return null;
	}

	@GetMapping("/r/{code}")
	public String redirectToOriginalUrl(@PathVariable("code") String code) {
		return null;
	}

	@GetMapping("urls/{code}")
	public ResponseEntity<UrlAccessLog> getAccessLog(@PathVariable("code") String code) {
		return null;
	}
}
