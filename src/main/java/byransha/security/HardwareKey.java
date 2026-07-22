package byransha.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.crypto.SecretKey;

/**
 * Local "Node State" and "Wallet Manager".
 * Manages a node's local cryptographic state using a secure
 * PKCS12 KeyStore.
 */
public class HardwareKey {

	private static final String KEYSTORE_TYPE = "PKCS12";
	private static final String KEYSTORE_FILE = "node_wallet.p12";
	private static final String SECRET_ALIAS = "node_local_encryption_key";
	private static final String IDENTITY_ALIAS = "node_mtls_identity";

	/**
	 * Derives or generates the local AES database encryption key.
	 */
	public static SecretKey deriveSecretKey() throws Exception {
		char[] pwd = getUnattendedPassphrase();
		KeyStore ks = loadKeyStore(pwd);
		KeyStore.PasswordProtection kp = new KeyStore.PasswordProtection(pwd);

		KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) ks.getEntry(SECRET_ALIAS, kp);
		if (entry != null) {
			return entry.getSecretKey();
		}

		// Generate new if missing
		SecretKey newKey = (SecretKey) AES.getRandomKey(256);
		ks.setEntry(SECRET_ALIAS, new KeyStore.SecretKeyEntry(newKey), kp);
		saveKeyStore(ks, pwd);

		return newKey;
	}

	/**
	 * Stores the node's asymmetric identity for mTLS.
	 */
	public static void storeIdentity(PrivateKey privateKey, X509Certificate[] certChain) throws Exception {
		char[] pwd = getUnattendedPassphrase();
		KeyStore ks = loadKeyStore(pwd);

		ks.setKeyEntry(IDENTITY_ALIAS, privateKey, pwd, certChain);
		saveKeyStore(ks, pwd);
	}

	/**
	 * Loads the node's mTLS Private Key.
	 */
	public static PrivateKey loadPrivateKey() throws Exception {
		char[] pwd = getUnattendedPassphrase();
		KeyStore ks = loadKeyStore(pwd);
		return (PrivateKey) ks.getKey(IDENTITY_ALIAS, pwd);
	}

	/**
	 * Loads the node's mTLS Certificate.
	 */
	public static X509Certificate loadCertificate() throws Exception {
		char[] pwd = getUnattendedPassphrase();
		KeyStore ks = loadKeyStore(pwd);
		Certificate cert = ks.getCertificate(IDENTITY_ALIAS);
		return (cert instanceof X509Certificate) ? (X509Certificate) cert : null;
	}

	/**
	 * Helper: Loads the KeyStore from disk, or initializes an empty one.
	 */
	private static KeyStore loadKeyStore(char[] pwd) throws Exception {
		KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE);
		File file = new File(KEYSTORE_FILE);

		if (file.exists()) {
			try (FileInputStream fis = new FileInputStream(file)) {
				ks.load(fis, pwd);
			}
		} else {
			ks.load(null, pwd);
		}
		return ks;
	}

	/**
	 * Helper: Saves the KeyStore state to disk.
	 */
	private static void saveKeyStore(KeyStore ks, char[] pwd) throws Exception {
		try (FileOutputStream fos = new FileOutputStream(KEYSTORE_FILE)) {
			ks.store(fos, pwd);
		}
	}

	/**
	 * Fetches the passphrase from the environment or returns a default.
	 */
	private static char[] getUnattendedPassphrase() {
		String envPass = System.getenv("NODE_WALLET_PASSWORD");
		if (envPass != null && !envPass.isBlank()) {
			return envPass.toCharArray();
		}
		// Fallback
		return "default_unattended_node_passphrase".toCharArray();
	}
}
