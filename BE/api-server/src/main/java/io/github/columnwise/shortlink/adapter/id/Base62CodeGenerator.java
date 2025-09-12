package io.github.columnwise.shortlink.adapter.id;

import io.github.columnwise.shortlink.domain.service.CodeGenerator;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
@Primary
public class Base62CodeGenerator implements CodeGenerator {
    
    private static final String BASE62_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = 62;
    
    @Override
    public String generate(String longUrl) {
        try {
            // URL을 SHA-256 해시로 변환
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(longUrl.getBytes());
            
            // 해시를 BigInteger로 변환 (양수로 만들기 위해 절댓값 사용)
            BigInteger hashInt = new BigInteger(1, hashBytes);
            
            // BigInteger를 Base62로 변환
            return toBase62(hashInt);
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    private String toBase62(BigInteger number) {
        if (number.equals(BigInteger.ZERO)) {
            return "0";
        }
        
        StringBuilder result = new StringBuilder();
        BigInteger base = BigInteger.valueOf(BASE);
        
        while (number.compareTo(BigInteger.ZERO) > 0) {
            int remainder = number.remainder(base).intValue();
            result.insert(0, BASE62_CHARS.charAt(remainder));
            number = number.divide(base);
        }
        
        // 최소 6자리로 패딩 (필요시)
        while (result.length() < 6) {
            result.insert(0, '0');
        }
        
        // 최대 10자리로 제한 (너무 길어지는 것 방지)
        if (result.length() > 10) {
            return result.substring(0, 10);
        }
        
        return result.toString();
    }
}
