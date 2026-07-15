package byransha.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.util.Collections;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.scp.client.ScpClient;
import org.apache.sshd.scp.client.ScpClientCreator;

public class MinaScpUploader {
	/**
	 * Uploads a file via SCP using Private Key Authentication.
	 *
	 * @param host           Remote hostname or IP.
	 * @param port           SSH Port (usually 22).
	 * @param username       SSH Username.
	 * @param privateKeyPath Path to your local private key file (e.g.,
	 *                       ~/.ssh/id_rsa).
	 * @param passphrase     Optional password if the private key is encrypted (pass
	 *                       null if unencrypted).
	 * @param localPath      Path to local file to upload.
	 * @param remotePath     Destination directory or file path on the remote
	 *                       server.
	 * @throws IOException If connection, authentication, or file transfer fails.
	 */
	public static void uploadWithPrivateKey(String host, int port, String username, String privateKeyPath,
			String passphrase, String localPath, String remotePath) throws IOException {

		Path localFile = Paths.get(localPath);
		Path keyFile = Paths.get(privateKeyPath);

		if (!Files.exists(localFile)) {
			throw new IllegalArgumentException("Local file does not exist: " + localPath);
		}
		if (!Files.exists(keyFile)) {
			throw new IllegalArgumentException("Private key file not found at: " + privateKeyPath);
		}

		// 1. Set up the client engine
		try (SshClient client = SshClient.setUpDefaultClient()) {

			// Optional: Define host key verification rules here if needed
			// client.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);

			client.start();

			// 2. Establish connection to host
			try (ClientSession session = client.connect(username, host, port).verify(10000).getSession()) {

				// 3. Parse and load the private key file
				FileKeyPairProvider keyPairProvider = new FileKeyPairProvider(keyFile);

				// If the key file is encrypted with a password passphrase, configure a password
				// provider
				if (passphrase != null && !passphrase.isEmpty()) {
					keyPairProvider.setPasswordFinder((s, resourceKey, retryIndex) -> passphrase);				}

				// Extract key pairs from file and add them to the active session identities
				Iterable<KeyPair> keys = keyPairProvider.loadKeys(session);
				for (KeyPair key : keys) {
					session.addPublicKeyIdentity(key);
				}

				// 4. Authenticate session using loaded keys
				session.auth().verify(10000);

				// 5. Initialize SCP Subsystem and execute upload
				ScpClientCreator creator = ScpClientCreator.instance();
				ScpClient scpClient = creator.createScpClient(session);

				scpClient.upload(localFile, remotePath, Collections.emptySet());
				System.out.println("File successfully uploaded to  " + host + ":" + remotePath);

			} finally {
				client.stop();
			}
		}
	}
}