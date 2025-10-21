package io.github.columnwise.shortlink.util;

import java.math.BigInteger;

/**
 * Base62 인코딩/디코딩 유틸리티
 *
 * <p>숫자를 Base62 문자열로 변환하거나 그 반대 변환을 수행합니다.
 * Base62는 0-9, A-Z, a-z 총 62개 문자를 사용하여 URL에 안전한 짧은 문자열을 생성합니다.</p>
 *
 * <p>사용 예:</p>
 * <pre>
 * String encoded = Base62Utils.encode(123456);  // "W7E"
 * long decoded = Base62Utils.decode("W7E");     // 123456
 * </pre>
 */
public class Base62Utils {

    private static final String BASE62_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = 62;

    private Base62Utils() {
        // Utility class - prevent instantiation
    }

    /**
     * 숫자를 Base62 문자열로 인코딩합니다.
     *
     * @param number 인코딩할 숫자 (0 이상)
     * @return Base62로 인코딩된 문자열
     * @throws IllegalArgumentException 음수가 입력된 경우
     */
    public static String encode(long number) {
        if (number < 0) {
            throw new IllegalArgumentException("Number must be non-negative");
        }

        if (number == 0) {
            return "0";
        }

        StringBuilder result = new StringBuilder();
        while (number > 0) {
            result.insert(0, BASE62_CHARS.charAt((int) (number % BASE)));
            number /= BASE;
        }

        return result.toString();
    }

    /**
     * BigInteger를 Base62 문자열로 인코딩합니다.
     *
     * @param number 인코딩할 BigInteger (0 이상)
     * @return Base62로 인코딩된 문자열
     * @throws IllegalArgumentException 음수가 입력된 경우
     */
    public static String encode(BigInteger number) {
        if (number == null) {
            throw new IllegalArgumentException("Number cannot be null");
        }
        if (number.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("Number must be non-negative");
        }

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

        return result.toString();
    }

    /**
     * Base62 문자열을 숫자로 디코딩합니다.
     *
     * @param encoded Base62로 인코딩된 문자열
     * @return 디코딩된 숫자
     * @throws IllegalArgumentException 잘못된 문자가 포함된 경우
     */
    public static long decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            throw new IllegalArgumentException("Encoded string cannot be null or empty");
        }

        long result = 0;
        long power = 1;

        for (int i = encoded.length() - 1; i >= 0; i--) {
            char c = encoded.charAt(i);
            int index = BASE62_CHARS.indexOf(c);
            if (index == -1) {
                throw new IllegalArgumentException("Invalid character in Base62 string: " + c);
            }
            result += index * power;
            power *= BASE;
        }

        return result;
    }

    /**
     * Base62 문자열을 BigInteger로 디코딩합니다.
     *
     * @param encoded Base62로 인코딩된 문자열
     * @return 디코딩된 BigInteger
     * @throws IllegalArgumentException 잘못된 문자가 포함된 경우
     */
    public static BigInteger decodeToBigInteger(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            throw new IllegalArgumentException("Encoded string cannot be null or empty");
        }

        BigInteger result = BigInteger.ZERO;
        BigInteger power = BigInteger.ONE;
        BigInteger base = BigInteger.valueOf(BASE);

        for (int i = encoded.length() - 1; i >= 0; i--) {
            char c = encoded.charAt(i);
            int index = BASE62_CHARS.indexOf(c);
            if (index == -1) {
                throw new IllegalArgumentException("Invalid character in Base62 string: " + c);
            }
            result = result.add(BigInteger.valueOf(index).multiply(power));
            power = power.multiply(base);
        }

        return result;
    }

    /**
     * Base62 문자열이 유효한지 검증합니다.
     *
     * @param encoded 검증할 문자열
     * @return 유효하면 true, 그렇지 않으면 false
     */
    public static boolean isValid(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return false;
        }

        for (char c : encoded.toCharArray()) {
            if (BASE62_CHARS.indexOf(c) == -1) {
                return false;
            }
        }

        return true;
    }
}