package io.academicmonitor.integration.idukay.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class CryptoJsPasswordEncoderTest {

    private final CryptoJsPasswordEncoder encoder = new CryptoJsPasswordEncoder();

    @Test
    void encodesUsingCryptoJsOpenSslCompatibleFormat() {

        char[] password = "Prueba123!".toCharArray();

        byte[] salt = hex("0011223344556677");

        String encoded = encoder.encode(password, salt);

        assertEquals("U2FsdGVkX18AESIzRFVmd+dlob9dYkRXketqpXWBJVc=", encoded);
    }

    @Test
    void encodedValueContainsOpenSslSaltedPrefix() {

        String encoded = encoder.encode("temporary-test-password".toCharArray());

        byte[] decoded = Base64.getDecoder().decode(encoded);

        String prefix = new String(decoded, 0, 8, StandardCharsets.US_ASCII);

        assertEquals("Salted__", prefix);
    }

    @Test
    void generatesDifferentCiphertextForSamePassword() {

        char[] password = "temporary-test-password".toCharArray();

        String first = encoder.encode(password);

        String second = encoder.encode(password);

        assertFalse(first.equals(second));
    }

    @Test
    void deterministicEncodingIsValidBase64() {

        String encoded = encoder.encode("Prueba123!".toCharArray(), hex("0011223344556677"));

        byte[] decoded = Base64.getDecoder().decode(encoded);

        assertTrue(decoded.length > 16);
    }

    @Test
    void rejectsNullPassword() {

        assertThrows(IllegalArgumentException.class, () -> encoder.encode(null));
    }

    @Test
    void rejectsInvalidSaltLength() {

        assertThrows(IllegalArgumentException.class, () -> encoder.encode("test".toCharArray(), new byte[7]));
    }

    private byte[] hex(String value) {

        int length = value.length();

        byte[] result = new byte[length / 2];

        for (int index = 0; index < length; index += 2) {

            result[index / 2] = (byte) Integer.parseInt(value.substring(index, index + 2), 16);
        }

        return result;
    }
}
