package byransha.security;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

/*
 * Usage example:
 *
 * // Server
 * new Thread(() -> {
 *     try (SSLSocket socket = secureLink.listen(8443)) {
 *         System.out.println("Peer connected!");
 *         socket.getOutputStream().write("Hello from Server".getBytes());
 *     } catch (Exception err) {
 *         err.printStackTrace();
 *     }
 * }).start();
 *
 * // Client
 * try (SSLSocket socket = secureLink.connect("192.168.1.50", 8443)) {
 *     System.out.println("Connected to Peer!");
 *     socket.getOutputStream().write("Hello from Client".getBytes());
 * } catch (Exception err) {
 *     err.printStackTrace();
 * }
 */

/**
 * Secure Socket Layer (SSL) wrapper for Mutual TLS (mTLS).
 */
public class SSL {

	private static final String TLS_VERSION = "TLSv1.3";
	private static final char[] DUMMY_PWD = "dummy".toCharArray();

	private final SSLContext sslContext;

	/**
	 * Initializes the secure mTLS context for the cluster.
	 *
	 * @param myKey
	 *                      The local node's private key.
	 * @param myCert
	 *                      The local node's public certificate.
	 * @param peerCerts
	 *                      Array of trusted peer certificates in the cluster.
	 */
	public SSL(PrivateKey myKey, X509Certificate myCert, X509Certificate[] peerCerts) {
		try {
			KeyManagerFactory kmf = initKeyManager(myKey, myCert);
			TrustManagerFactory tmf = initTrustManager(peerCerts);

			this.sslContext = SSLContext.getInstance(TLS_VERSION);
			this.sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
		} catch (Exception err) {
			throw new SecurityException("Failed to initialize mTLS context", err);
		}
	}

	/**
	 * Creates and initializes the KeyManager for the local
	 * node's identity.
	 */
	private KeyManagerFactory initKeyManager(PrivateKey myKey, X509Certificate myCert) throws Exception {
		KeyStore identityStore = KeyStore.getInstance(KeyStore.getDefaultType());
		identityStore.load(null, null);
		identityStore.setKeyEntry("local-node", myKey, DUMMY_PWD, new X509Certificate[] { myCert });

		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(identityStore, DUMMY_PWD);
		return kmf;
	}

	/**
	 * Creates and initializes the TrustManager to pin the entire
	 * cluster's certificates.
	 */
	private TrustManagerFactory initTrustManager(X509Certificate[] peerCerts) throws Exception {
		KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
		trustStore.load(null, null);

		for (int i = 0; i < peerCerts.length; i++) {
			trustStore.setCertificateEntry("trusted-peer-" + i, peerCerts[i]);
		}

		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(trustStore);
		return tmf;
	}

	/**
	 * Listens for incoming mTLS connections from trusted peers.
	 */
	public SSLSocket listen(int port) {
		try {
			SSLServerSocket server = (SSLServerSocket) sslContext.getServerSocketFactory().createServerSocket(port);
			server.setNeedClientAuth(true); // Force mTLS

			SSLSocket socket = (SSLSocket) server.accept();
			socket.startHandshake();
			return socket;
		} catch (Exception err) {
			throw new SecurityException("Failed to listen for mTLS connection on port " + port, err);
		}
	}

	public SSLSocket connect(String host, int port) {
		try {
			SSLSocket socket = (SSLSocket) sslContext.getSocketFactory().createSocket(host, port);
			socket.startHandshake();
			return socket;
		} catch (Exception err) {
			throw new SecurityException("Failed to connect to mTLS peer", err);
		}
	}

	/**
	 * Utility to import an offline-exchanged X.509 Certificate from
	 * a PEM string.
	 *
	 * @param pem
	 *                The PEM formatted certificate string.
	 * @return The parsed X509Certificate.
	 */
	public static X509Certificate certFromCertificatePEM(String pem) {
		try (InputStream is = new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8))) {
			CertificateFactory fact = CertificateFactory.getInstance("X.509");
			return (X509Certificate) fact.generateCertificate(is);
		} catch (Exception err) {
			throw new SecurityException("Failed to parse PEM certificate", err);
		}
	}
}
