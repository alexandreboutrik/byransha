package byransha.security;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;

/**
 * Rivest-Shamir-Adleman (RSA) cryptographic utility class. Provides
 * implementations for Key Generation, Encryption, and Decryption. Designed as a
 * legacy fallback for environments where Elliptic Curve Cryptography (ECC) is
 * not supported.
 */
public class RSA {

	private static final String ALGORITHM = "RSA";

	// Uses OAEP padding to prevent padding oracle attacks
	// (PKCS#1 v1.5 is insecure)
	private static final String TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

	private static final int KEY_SIZE = 3072;

	public static KeyPair randomKeyPair() {
		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
			keyPairGenerator.initialize(KEY_SIZE, new SecureRandom());
			return keyPairGenerator.generateKeyPair();
		} catch (Exception err) {
			throw new SecurityException("Failed to generate RSA KeyPair", err);
		}
	}

	public static byte[] encrypt(byte[] plainData, Key key) {
		try {
			// Cipher is thread-unsafe and must be instantiated
			// per operation
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, key);
			return cipher.doFinal(plainData);
		} catch (Exception err) {
			throw new SecurityException("RSA Encryption failed", err);
		}
	}

	public static byte[] decrypt(byte[] cipherData, Key key) {
		try {
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, key);
			return cipher.doFinal(cipherData);
		} catch (Exception err) {
			throw new SecurityException("RSA Decryption failed", err);
		}
	}

	public static String toPem(Key key) {
		if (key == null) {
			throw new IllegalArgumentException("Key cannot be null");
		}

		String header;
		String footer;

		if (key instanceof PublicKey) {
			header = "-----BEGIN PUBLIC KEY-----";
			footer = "-----END PUBLIC KEY-----";
		} else if (key instanceof PrivateKey) {
			header = "-----BEGIN PRIVATE KEY-----";
			footer = "-----END PRIVATE KEY-----";
		} else {
			throw new IllegalArgumentException("Unsupported key type: " + key.getClass().getName());
		}

		// Get the standard encoded bytes (X.509 for Public, PKCS#8 for Private)
		byte[] encodedKey = key.getEncoded();
		if (encodedKey == null) {
			throw new IllegalArgumentException("Key does not support encoding (getEncoded() returned null)");
		}

		// Base64 encode with MIME line breaks (64 characters per line)
		String base64 = Base64.getMimeEncoder(64, new byte[] { '\n' }).encodeToString(encodedKey);

		return header + "\n" + base64 + "\n" + footer;
	}

	/**
	 * Converts a PEM-formatted string (Public or Private Key) back into a Java Key
	 * object.
	 *
	 * @param pem       The PEM string containing the key.
	 * @param algorithm Key algorithm (e.g., "RSA", "EC", "DSA"). Defaults to "RSA"
	 *                  if null.
	 * @return A PublicKey or PrivateKey object.
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeySpecException 
	 */
	public static Key fromPem(String pem) throws NoSuchAlgorithmException, InvalidKeySpecException  {
		if (pem == null || pem.trim().isEmpty()) {
			throw new IllegalArgumentException("PEM string cannot be null or empty");
		}

		String keyAlgorithm = "RSA";
		KeyFactory keyFactory = KeyFactory.getInstance(keyAlgorithm);

		if (pem.contains("-----BEGIN PUBLIC KEY-----")) {
			// Clean up headers, footers, and whitespace
			String base64 = pem.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "")
					.replaceAll("\\s+", "");

			byte[] decoded = Base64.getDecoder().decode(base64);
			X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
			return keyFactory.generatePublic(spec);

		} else if (pem.contains("-----BEGIN PRIVATE KEY-----")) {
			// Clean up headers, footers, and whitespace
			String base64 = pem.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "")
					.replaceAll("\\s+", "");

			byte[] decoded = Base64.getDecoder().decode(base64);
			PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
			return keyFactory.generatePrivate(spec);

		} else if (pem.contains("-----BEGIN RSA PRIVATE KEY-----")) {
			throw new IllegalArgumentException("Legacy PKCS#1 format ('BEGIN RSA PRIVATE KEY') detected. "
					+ "Convert to PKCS#8 ('BEGIN PRIVATE KEY') using OpenSSL or Bouncy Castle.");
		} else {
			throw new IllegalArgumentException("Unrecognized or unsupported PEM header");
		}
	}

	public static String toBase64(Key key) {
		return Base64.getEncoder().encodeToString(key.getEncoded());
	}

	public static String toPem(PublicKey key) {
		String base64 = Base64.getMimeEncoder(64, new byte[] { '\n' }).encodeToString(key.getEncoded());
		return "-----BEGIN PUBLIC KEY-----\n" + base64 + "\n-----END PUBLIC KEY-----";
	}

	public static String toOpenSsh(PublicKey key) {
		if (!(key instanceof RSAPublicKey rsa)) {
			throw new IllegalArgumentException("Provided key is not an RSAPublicKey.");
		}

		// Try-with-resources ensures streams are automatically closed,
		// preventing memory leaks
		try (ByteArrayOutputStream buf = new ByteArrayOutputStream();
				DataOutputStream out = new DataOutputStream(buf)) {

			byte[] alg = "ssh-rsa".getBytes();
			out.writeInt(alg.length);
			out.write(alg);

			byte[] e = rsa.getPublicExponent().toByteArray();
			out.writeInt(e.length);
			out.write(e);

			byte[] n = rsa.getModulus().toByteArray();
			out.writeInt(n.length);
			out.write(n);

			return "ssh-rsa " + Base64.getEncoder().encodeToString(buf.toByteArray());
		} catch (Exception err) {
			throw new SecurityException("Failed to format key as OpenSSH", err);
		}
	}
}
