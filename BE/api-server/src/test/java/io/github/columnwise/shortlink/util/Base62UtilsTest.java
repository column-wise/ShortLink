package io.github.columnwise.shortlink.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Base62Utils 유닛 테스트
 */
class Base62UtilsTest {

    @Test
    @DisplayName("기본적인 encode/decode 테스트")
    void testBasicEncodeDecode() {
        // Given
        long[] testNumbers = {0, 1, 61, 62, 123, 3844, 238328};

        for (long number : testNumbers) {
            // When
            String encoded = Base62Utils.encode(number);
            long decoded = Base62Utils.decode(encoded);

            // Then
            assertEquals(number, decoded, "Number " + number + " should encode and decode correctly");
        }
    }

    @Test
    @DisplayName("BigInteger encode/decode 테스트")
    void testBigIntegerEncodeDecode() {
        // Given
        BigInteger bigNumber = new BigInteger("123456789012345678901234567890");

        // When
        String encoded = Base62Utils.encode(bigNumber);
        BigInteger decoded = Base62Utils.decodeToBigInteger(encoded);

        // Then
        assertEquals(bigNumber, decoded);
    }

    @Test
    @DisplayName("특정 값들의 인코딩 결과 검증")
    void testSpecificEncodings() {
        assertEquals("0", Base62Utils.encode(0));
        assertEquals("1", Base62Utils.encode(1));
        assertEquals("z", Base62Utils.encode(61));
        assertEquals("10", Base62Utils.encode(62));
        assertEquals("1z", Base62Utils.encode(123));
    }

    @Test
    @DisplayName("isValid 메서드 테스트")
    void testIsValid() {
        assertTrue(Base62Utils.isValid("abc123"));
        assertTrue(Base62Utils.isValid("ABC"));
        assertTrue(Base62Utils.isValid("0"));
        assertTrue(Base62Utils.isValid("zZ9"));

        assertFalse(Base62Utils.isValid(""));
        assertFalse(Base62Utils.isValid(null));
        assertFalse(Base62Utils.isValid("abc-123"));
        assertFalse(Base62Utils.isValid("abc@123"));
        assertFalse(Base62Utils.isValid("한글"));
    }

    @Test
    @DisplayName("음수 입력시 예외 발생")
    void testNegativeNumberThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> Base62Utils.encode(-1));
        assertThrows(IllegalArgumentException.class, () -> Base62Utils.encode(BigInteger.valueOf(-1)));
    }

    @Test
    @DisplayName("null BigInteger 입력시 예외 발생")
    void testNullBigIntegerThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> Base62Utils.encode((BigInteger) null));
    }

    @Test
    @DisplayName("잘못된 문자열 디코딩시 예외 발생")
    void testInvalidStringDecodeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> Base62Utils.decode("abc@123"));
        assertThrows(IllegalArgumentException.class, () -> Base62Utils.decode(""));
        assertThrows(IllegalArgumentException.class, () -> Base62Utils.decode(null));

        assertThrows(IllegalArgumentException.class, () -> Base62Utils.decodeToBigInteger("abc@123"));
        assertThrows(IllegalArgumentException.class, () -> Base62Utils.decodeToBigInteger(""));
        assertThrows(IllegalArgumentException.class, () -> Base62Utils.decodeToBigInteger(null));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1, 10, 100, 1000, 10000, 100000, 1000000, Long.MAX_VALUE})
    @DisplayName("다양한 크기의 숫자에 대한 encode/decode 테스트")
    void testVariousSizes(long number) {
        String encoded = Base62Utils.encode(number);
        long decoded = Base62Utils.decode(encoded);
        assertEquals(number, decoded);
    }

    @Test
    @DisplayName("큰 숫자의 인코딩 길이가 합리적인지 검증")
    void testEncodingLength() {
        // 작은 숫자는 짧게 인코딩되어야 함
        assertTrue(Base62Utils.encode(0).length() <= 2);
        assertTrue(Base62Utils.encode(61).length() <= 2);

        // 중간 크기 숫자
        assertTrue(Base62Utils.encode(1000000).length() <= 10);

        // 큰 숫자도 적당한 길이여야 함
        assertTrue(Base62Utils.encode(Long.MAX_VALUE).length() <= 20);
    }

    @Test
    @DisplayName("Base62 문자셋 검증")
    void testCharacterSet() {
        // 모든 Base62 문자가 올바르게 인코딩/디코딩되는지 확인
        for (int i = 0; i < 62; i++) {
            String encoded = Base62Utils.encode(i);
            long decoded = Base62Utils.decode(encoded);
            assertEquals(i, decoded);
        }
    }
}