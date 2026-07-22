package byransha.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Advanced Encryption Standard (AES) utility class.
 * Implements AES with Galois/Counter Mode (GCM) for authenticated
 * encryption.
 */
public class AES {

	private static final String ALGO = "AES";
	private static final String TRANSFORMATION = "AES/GCM/NoPadding";
	private static final int GCM_IV_LENGTH = 12;
	private static final int GCM_TAG_LENGTH = 128;

	public static byte[] encrypt(byte[] data, Key key) {
		try {
			byte[] iv = new byte[GCM_IV_LENGTH];
			new SecureRandom().nextBytes(iv);

			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
			cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

			byte[] cipherText = cipher.doFinal(data);

			ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
			byteBuffer.put(iv);
			byteBuffer.put(cipherText);
			return byteBuffer.array();
		} catch (Exception err) {
			throw new SecurityException("AES encryption failed", err);
		}
	}

	public static byte[] decrypt(byte[] data, Key key) {
		if (data.length < GCM_IV_LENGTH) {
			throw new SecurityException("Invalid ciphertext: too short to contain IV");
		}

		try {
			byte[] iv = new byte[GCM_IV_LENGTH];
			System.arraycopy(data, 0, iv, 0, iv.length);

			byte[] cipherText = new byte[data.length - GCM_IV_LENGTH];
			System.arraycopy(data, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
			cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

			return cipher.doFinal(cipherText);
		} catch (Exception err) {
			throw new SecurityException("AES decryption failed", err);
		}
	}

	/**
	 * Generates a cryptographically secure random (CSRNG) AES key.
	 *
	 * @param keySize
	 *                    The key size in bits (must be 128, 192, or 256).
	 * @return A randomly generated SecretKey.
	 */
	public static Key getRandomKey(int keySize) {
		if (keySize != 128 && keySize != 192 && keySize != 256) {
			throw new IllegalArgumentException("Invalid AES key size. Must be 128, 192, or 256.");
		}
		byte[] randomKeyBytes = new byte[keySize / 8];
		new SecureRandom().nextBytes(randomKeyBytes);
		return new SecretKeySpec(randomKeyBytes, ALGO);
	}

	/**
	 * Derives a stable cluster/node encryption key.
	 *
	 * @return A SecretKey based on distributed node parameters.
	 */
	public static Key createStringBasedOnHardware() {
		try {
			return HardwareKey.deriveSecretKey();
		} catch (Exception e) {
			throw new SecurityException("Failed to derive node key", e);
		}
	}

	/**
	 * Creates a stable 256-bit AES key by hashing the input string
	 * with SHA-256.
	 *
	 * @param s
	 *              The input string to derive the key from.
	 * @return A valid 256-bit SecretKey.
	 */
	public static Key createKey(String s) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] keyBytes = digest.digest(s.getBytes(StandardCharsets.UTF_8));
			return new SecretKeySpec(keyBytes, ALGO);
		} catch (Exception e) {
			throw new SecurityException("Failed to create key from string", e);
		}
	}
}
