package byransha.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;

import javax.crypto.SecretKey;

/**
 * Local "Node State" and "Wallet Manager".
 * Manages a node's local cryptographic state using a secure
 * PKCS12 KeyStore.
 */
public class HardwareKey {

	private static final String KEYSTORE_TYPE = "PKCS12";
	private static final String KEYSTORE_FILE = "node_wallet.p12";
	private static final String KEY_ALIAS = "node_local_encryption_key";

	public static SecretKey derive() throws Exception {
		char[] passphrase = getUnattendedPassphrase();

		File ksFile = new File(KEYSTORE_FILE);
		KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
		KeyStore.PasswordProtection keyPassword = new KeyStore.PasswordProtection(passphrase);

		if (ksFile.exists()) {
			return loadExistingKey(ksFile, keyStore, passphrase, keyPassword);
		}

		return generateAndStoreKey(ksFile, keyStore, passphrase, keyPassword);
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

	/**
	 * Loads an existing SecretKey from the local PKCS12 KeyStore.
	 *
	 * @param file
	 *                 The KeyStore file on disk.
	 * @param ks
	 *                 The KeyStore instance.
	 * @param pwd
	 *                 The node's passphrase.
	 * @param kp
	 *                 The KeyStore password protection parameter.
	 * @return The retrieved SecretKey.
	 * @throws IllegalStateException
	 *                                   If the KeyStore exists but the
	 *                                   expected key alias is missing.
	 * @throws Exception
	 *                                   If file reading or KeyStore unlocking
	 *                                   fails.
	 */
	private static SecretKey loadExistingKey(File file, KeyStore ks, char[] pwd, KeyStore.PasswordProtection kp)
			throws Exception {
		try (FileInputStream fis = new FileInputStream(file)) {
			ks.load(fis, pwd);
		}

		KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) ks.getEntry(KEY_ALIAS, kp);
		if (entry != null) {
			return entry.getSecretKey();
		}

		throw new IllegalStateException("Node Wallet exists but the encryption key is missing.");
	}

	/**
	 * Generates a new AES key, initializes the KeyStore, and saves
	 * it to disk.
	 *
	 * @param file
	 *                 The KeyStore file to create.
	 * @param ks
	 *                 The KeyStore instance.
	 * @param pwd
	 *                 The node's passphrase.
	 * @param kp
	 *                 The KeyStore password protection parameter.
	 * @return The newly generated SecretKey.
	 * @throws Exception
	 *                       If key generation, KeyStore initialization,
	 *                       or file writing fails.
	 */
	private static SecretKey generateAndStoreKey(File file, KeyStore ks, char[] pwd, KeyStore.PasswordProtection kp)
			throws Exception {
		ks.load(null, pwd);

		SecretKey newKey = (SecretKey) AES.getRandomKey(256);
		KeyStore.SecretKeyEntry entry = new KeyStore.SecretKeyEntry(newKey);

		ks.setEntry(KEY_ALIAS, entry, kp);

		try (FileOutputStream fos = new FileOutputStream(file)) {
			ks.store(fos, pwd);
		}

		return newKey;
	}
}
