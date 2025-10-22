package io.github.columnwise.shortlink.adapter.persistence;

import io.github.columnwise.shortlink.adapter.persistence.entity.ShortUrlEntity;
import io.github.columnwise.shortlink.application.port.out.ShortUrlRepositoryPort;
import io.github.columnwise.shortlink.domain.model.ShortUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ShortUrl 리포지토리 어댑터 구현체
 *
 * <p>Spring Data JPA를 사용하여 ShortUrlRepositoryPort를 구현하는 어댑터입니다.
 * 도메인 모델과 JPA 엔티티 간의 변환을 담당합니다.</p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>ShortUrl 도메인 모델 저장 및 조회</li>
 *   <li>URL 접속 로그 저장</li>
 *   <li>도메인 모델과 엔티티 간 변환</li>
 *   <li>단축 코드 및 원본 URL로 조회</li>
 * </ul>
 */
@Repository
@RequiredArgsConstructor
public class ShortUrlRepositoryAdapter implements ShortUrlRepositoryPort {
    
    private final SpringDataShortUrlRepository shortUrlRepository;
    
    /**
     * ShortUrl 도메인 모델을 데이터베이스에 저장합니다.
     *
     * <p>도메인 모델을 JPA 엔티티로 변환하여 저장하고,
     * 저장된 결과를 다시 도메인 모델로 변환하여 반환합니다.</p>
     *
     * @param shortUrl 저장할 ShortUrl 도메인 모델
     * @return 저장된 ShortUrl 도메인 모델 (ID 포함)
     */
    @Override
    public ShortUrl save(ShortUrl shortUrl) {
        ShortUrlEntity entity = ShortUrlEntity.builder()
                .code(shortUrl.code())
                .longUrl(shortUrl.longUrl())
                .createdAt(shortUrl.createdAt())
                .expiresAt(shortUrl.expiresAt())
                .isActive(shortUrl.isActive())
                .build();
                
        ShortUrlEntity saved = shortUrlRepository.save(entity);
        
        return ShortUrl.builder()
                .id(saved.getId())
                .code(saved.getCode())
                .longUrl(saved.getLongUrl())
                .createdAt(saved.getCreatedAt())
                .expiresAt(saved.getExpiresAt())
                .isActive(saved.isActive())
                .build();
    }
    
    /**
     * 단축 코드로 ShortUrl을 조회합니다.
     *
     * @param code 조회할 단축 코드
     * @return 조회된 ShortUrl 도메인 모델 (Optional)
     */
    @Override
    public Optional<ShortUrl> findByCode(String code) {
        return shortUrlRepository.findByCode(code)
                .map(entity -> ShortUrl.builder()
                        .id(entity.getId())
                        .code(entity.getCode())
                        .longUrl(entity.getLongUrl())
                        .createdAt(entity.getCreatedAt())
                        .expiresAt(entity.getExpiresAt())
                        .isActive(entity.isActive())
                        .build());
    }
    
    
    /**
     * 원본 URL로 ShortUrl을 조회합니다.
     *
     * @param longUrl 조회할 원본 URL
     * @return 조회된 ShortUrl 도메인 모델 (Optional)
     */
    @Override
    public Optional<ShortUrl> findByLongUrl(String longUrl) {
        return shortUrlRepository.findByLongUrl(longUrl)
                .map(entity -> ShortUrl.builder()
                        .id(entity.getId())
                        .code(entity.getCode())
                        .longUrl(entity.getLongUrl())
                        .createdAt(entity.getCreatedAt())
                        .expiresAt(entity.getExpiresAt())
                        .isActive(entity.isActive())
                        .build());
    }
    
}
