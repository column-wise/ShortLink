package io.github.columnwise.shortlink.adapter.id;

import io.github.columnwise.shortlink.domain.service.CodeGenerator;
import io.github.columnwise.shortlink.util.Base62Utils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Base62 기반 코드 생성기
 *
 * <p>SHA-256 해시를 Base62로 인코딩하여 URL에 안전한 짧은 코드를 생성합니다.
 * 동일한 URL에 대해서는 항상 동일한 코드를 생성합니다.</p>
 *
 * <p>생성되는 코드 특성:</p>
 * <ul>
 *   <li>최소 6자리, 최대 10자리</li>
 *   <li>0-9, A-Z, a-z 문자만 사용</li>
 *   <li>결정적 생성 (같은 입력 → 같은 출력)</li>
 * </ul>
 */
@Component
@Primary
public class Base62CodeGenerator implements CodeGenerator {

    @Override
    public String generate(String longUrl) {
        try {
            // URL을 SHA-256 해시로 변환
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(longUrl.getBytes());

            // 해시를 BigInteger로 변환 (양수로 만들기 위해 절댓값 사용)
            BigInteger hashInt = new BigInteger(1, hashBytes);

            // BigInteger를 Base62로 변환
            String base62 = Base62Utils.encode(hashInt);

            // 최소 6자리로 패딩 (필요시)
            while (base62.length() < 6) {
                base62 = "0" + base62;
            }

            // 최대 10자리로 제한 (너무 길어지는 것 방지)
            if (base62.length() > 10) {
                base62 = base62.substring(0, 10);
            }

            return base62;

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
