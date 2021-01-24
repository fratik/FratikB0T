/*
 * Copyright (C) 2019-2021 FratikB0T Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package pl.fratik.core.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

public class AES {

    private static final SecureRandom sr = new SecureRandom();

    private AES() {}

    public static String encryptAsB64(byte[] toEncrypt, String secret) throws CryptoException {
        return Base64.getEncoder().encodeToString(encrypt(toEncrypt, secret));
    }

    public static byte[] encrypt(byte[] toEncrypt, String secret) throws CryptoException {
        return encrypt(toEncrypt, secret.toCharArray());
    }

    public static byte[] encrypt(byte[] toEncrypt, char[] secret) throws CryptoException {
        try {
            byte[] iv = new byte[12];
            sr.nextBytes(iv);
            SecretKeySpec secretKey = generateKey(secret, iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(toEncrypt);
            ByteBuffer byteBuffer = ByteBuffer.allocate(4 + iv.length + encrypted.length);
            byteBuffer.putInt(iv.length);
            byteBuffer.put(iv);
            byteBuffer.put(encrypted);
            return byteBuffer.array();
        } catch (Exception e) {
            throw new CryptoException(e);
        }
    }

    private static SecretKeySpec generateKey(char[] secret, byte[] iv) throws CryptoException {
        try {
            KeySpec spec = new PBEKeySpec(secret, iv, 65536, 256); // AES-256
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] key = secretKeyFactory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(key, "AES");
        } catch (Exception e) {
            throw new CryptoException(e);
        }
    }

    public static String encryptAsB64(String toEncrypt, String secret) throws CryptoException {
        return encryptAsB64(toEncrypt.getBytes(StandardCharsets.UTF_8), secret);
    }

    public static byte[] encrypt(String toEncrypt, String secret) throws CryptoException {
        return encrypt(toEncrypt.getBytes(StandardCharsets.UTF_8), secret);
    }

    public static byte[] decrypt(byte[] toDecrypt, String secret) throws CryptoException {
        return decrypt(toDecrypt, secret.toCharArray());
    }

    public static byte[] decrypt(byte[] toDecrypt, char[] secret) throws CryptoException {
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(toDecrypt);
            int noonceSize = byteBuffer.getInt();
            if(noonceSize < 12 || noonceSize >= 16) {
                throw new IllegalArgumentException("Rozmiar nonce jest nieprawidłowy. To nie są dane zaszyfrowane metodą AES.");
            }
            byte[] iv = new byte[noonceSize];
            byteBuffer.get(iv);
            SecretKeySpec secretKey = generateKey(secret, iv);
            byte[] cipherBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherBytes);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
            return cipher.doFinal(cipherBytes);
        } catch (Exception e) {
            throw new CryptoException(e);
        }
    }

    public static byte[] decrypt(String toDecrypt, String secret) throws CryptoException {
        return decrypt(toDecrypt.getBytes(StandardCharsets.UTF_8), secret);
    }

    public static byte[] decrypt(String toDecrypt, char[] secret) throws CryptoException {
        return decrypt(toDecrypt.getBytes(StandardCharsets.UTF_8), secret);
    }

    public static byte[] decryptFromB64(byte[] toDecrypt, String secret) throws CryptoException {
        return decrypt(Base64.getDecoder().decode(toDecrypt), secret);
    }

    public static byte[] decryptFromB64(String toDecrypt, String secret) throws CryptoException {
        return decryptFromB64(toDecrypt.getBytes(StandardCharsets.UTF_8), secret);
    }
}
