package io.academicmonitor.integration.idukay.auth;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class CryptoJsPasswordEncoder {

    private static final String PASSPHRASE = "idukay-secret-key-password";

    private static final byte[] OPENSSL_MAGIC = "Salted__".getBytes(StandardCharsets.US_ASCII);

    private static final int SALT_LENGTH = 8;
    private static final int KEY_LENGTH = 32;
    private static final int IV_LENGTH = 16;

    private final SecureRandom secureRandom = new SecureRandom();

    public String encode(char[] password) {

        if (password == null) {
            throw new IllegalArgumentException("password is required");
        }

        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);

        try {
            return encode(password, salt);
        } finally {
            Arrays.fill(salt, (byte) 0);
        }
    }

    /*
     * Package-private overload used only to verify compatibility
     * against deterministic CryptoJS/OpenSSL test vectors.
     */
    String encode(char[] password, byte[] salt) {

        if (password == null) {
            throw new IllegalArgumentException("password is required");
        }

        if (salt == null || salt.length != SALT_LENGTH) {
            throw new IllegalArgumentException("salt must contain exactly 8 bytes");
        }

        byte[] plaintext = utf8(password);
        byte[] passphrase = PASSPHRASE.getBytes(StandardCharsets.UTF_8);

        byte[] derived = null;
        byte[] key = null;
        byte[] iv = null;

        try {
            derived = evpBytesToKey(passphrase, salt, KEY_LENGTH + IV_LENGTH);

            key = Arrays.copyOfRange(derived, 0, KEY_LENGTH);

            iv = Arrays.copyOfRange(derived, KEY_LENGTH, KEY_LENGTH + IV_LENGTH);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));

            byte[] ciphertext = cipher.doFinal(plaintext);

            byte[] output = new byte[OPENSSL_MAGIC.length + SALT_LENGTH + ciphertext.length];

            System.arraycopy(OPENSSL_MAGIC, 0, output, 0, OPENSSL_MAGIC.length);

            System.arraycopy(salt, 0, output, OPENSSL_MAGIC.length, SALT_LENGTH);

            System.arraycopy(ciphertext, 0, output, OPENSSL_MAGIC.length + SALT_LENGTH, ciphertext.length);

            try {
                return Base64.getEncoder().encodeToString(output);
            } finally {
                Arrays.fill(ciphertext, (byte) 0);
                Arrays.fill(output, (byte) 0);
            }

        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to encode password using " + "CryptoJS-compatible AES", exception);

        } finally {
            Arrays.fill(plaintext, (byte) 0);
            Arrays.fill(passphrase, (byte) 0);

            if (derived != null) {
                Arrays.fill(derived, (byte) 0);
            }

            if (key != null) {
                Arrays.fill(key, (byte) 0);
            }

            if (iv != null) {
                Arrays.fill(iv, (byte) 0);
            }
        }
    }

    private byte[] evpBytesToKey(byte[] passphrase, byte[] salt, int requiredLength) throws NoSuchAlgorithmException {

        MessageDigest md5 = MessageDigest.getInstance("MD5");

        byte[] derived = new byte[requiredLength];

        byte[] previous = new byte[0];

        int offset = 0;

        try {
            while (offset < requiredLength) {

                md5.reset();

                if (previous.length > 0) {
                    md5.update(previous);
                }

                md5.update(passphrase);
                md5.update(salt);

                byte[] current = md5.digest();

                int copyLength = Math.min(current.length, requiredLength - offset);

                System.arraycopy(current, 0, derived, offset, copyLength);

                offset += copyLength;

                Arrays.fill(previous, (byte) 0);
                previous = current;
            }

            return derived;

        } finally {
            Arrays.fill(previous, (byte) 0);
        }
    }

    private byte[] utf8(char[] value) {

        ByteBuffer buffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(value));

        byte[] bytes = new byte[buffer.remaining()];

        buffer.get(bytes);

        if (buffer.hasArray()) {
            Arrays.fill(buffer.array(), (byte) 0);
        }

        return bytes;
    }
}
