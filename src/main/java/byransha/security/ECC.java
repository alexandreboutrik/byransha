package byransha.security;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

import javax.crypto.KeyAgreement;

/**
 * Elliptic Curve Cryptography (ECC) utility class.
 * Based on the Bernstein principle. Uses Ed25519 (EdDSA) for Digital
 * Signatures and X25519 (XDH) for Key Agreement.
 */
public class ECC {

	private static final String SIGNATURE_ALGO = "Ed25519";
	private static final String KEY_AGREEMENT_KEY_ALGO = "X25519";
	private static final String KEY_AGREEMENT_ALGO = "XDH";

	public static KeyPair generateSignatureKeyPair() {
		try {
			return KeyPairGenerator.getInstance(SIGNATURE_ALGO).generateKeyPair();
		} catch (NoSuchAlgorithmException err) {
			throw new SecurityException("Failed to generate Ed25519 Signature KeyPair", err);
		}
	}

	public static KeyPair generateAgreementKeyPair() {
		try {
			return KeyPairGenerator.getInstance(KEY_AGREEMENT_KEY_ALGO).generateKeyPair();
		} catch (NoSuchAlgorithmException err) {
			throw new SecurityException("Failed to generate X25519 Agreement KeyPair", err);
		}
	}

	public static byte[] sign(byte[] data, PrivateKey privateKey) {
		try {
			Signature signature = Signature.getInstance(SIGNATURE_ALGO);
			signature.initSign(privateKey);
			signature.update(data);
			return signature.sign();
		} catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException err) {
			throw new SecurityException("Failed to sign data using Ed25519", err);
		}
	}

	public static boolean verify(byte[] data, byte[] signature, PublicKey publicKey) {
		try {
			Signature sigInstance = Signature.getInstance(SIGNATURE_ALGO);
			sigInstance.initVerify(publicKey);
			sigInstance.update(data);
			return sigInstance.verify(signature);
		} catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException err) {
			throw new SecurityException("Failed to verify Ed25519 signature", err);
		}
	}

	/**
	 * Derives a shared symmetric secret using X25519 (XDH).
	 *
	 * @param myPrivateKey
	 *                           The local node's X25519 private key.
	 * @param otherPublicKey
	 *                           The remote node's X25519 public key.
	 * @return A shared byte array (can be hashed and used as an AES key).
	 * @throws SecurityException
	 *                               If it fails.
	 */
	public static byte[] deriveSharedSecret(PrivateKey myPrivateKey, PublicKey otherPublicKey) {
		try {
			KeyAgreement keyAgreement = KeyAgreement.getInstance(KEY_AGREEMENT_ALGO);
			keyAgreement.init(myPrivateKey);
			keyAgreement.doPhase(otherPublicKey, true);
			return keyAgreement.generateSecret();
		} catch (NoSuchAlgorithmException | InvalidKeyException err) {
			throw new SecurityException("Failed to derive X25519 shared secret", err);
		}
	}
}
