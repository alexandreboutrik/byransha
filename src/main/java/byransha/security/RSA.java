package byransha.security;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;

/**
 * Rivest-Shamir-Adleman (RSA) cryptographic utility class.
 * Provides implementations for Key Generation, Encryption, and Decryption.
 * Designed as a legacy fallback for environments where Elliptic Curve
 * Cryptography (ECC) is not supported.
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

	/**
	 * Imports a PublicKey from an offline-exchanged PEM string.
	 */
	public static PublicKey fromPem(String pem) {
		try {
			String base64 = pem.replaceAll("-----(BEGIN|END) PUBLIC KEY-----", "").replaceAll("\\s", "");
			byte[] decoded = Base64.getDecoder().decode(base64);

			X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
			KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
			return kf.generatePublic(spec);
		} catch (Exception err) {
			throw new SecurityException("Failed to parse RSA public key from PEM", err);
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
