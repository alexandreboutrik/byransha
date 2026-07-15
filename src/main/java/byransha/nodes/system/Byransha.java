package byransha.nodes.system;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import byransha.VersionNode;
import byransha.graph.BGraph;
import byransha.graph.ShowInKishanView;
import byransha.nodes.primitive.URLNode;
import byransha.util.ByUtils;
import byransha.util.Version;

public class Byransha extends SystemNode {

	@ShowInKishanView
	public final URLNode sourceRepoURL = new URLNode(this, "https://github.com/lhogie/byransha");

	@ShowInKishanView
	public static final File homeDirectory = new File(ByUtils.home, ".byransha");
	@ShowInKishanView
	public static final File binDirectory = new File(homeDirectory, "bin");
	@ShowInKishanView
	public static final String homepage = "https://webusers.i3s.unice.fr/~hogie/software/byransha/";
	public static final String downloads = homepage + "/downloads/";
	public static final String downloadBinaries = downloads + "bin/";
	public static final String lastVersionURL = downloadBinaries + "info.json";
	public static byte[] currentExeBytes = "".getBytes();
	@ShowInKishanView
	public final VersionNode versionNode = new VersionNode(this);

	public Byransha(BGraph g) {
		super(g);

		new Thread(() -> {
			while (true) {
				try {
					var versionOnline = lastVersionOnline();

					if (versionOnline.isNewerThan(versionNode.version)) {
						System.out.println("New version available: " + versionOnline);
					}

					Thread.sleep(10000);
				} catch (IOException | InterruptedException | KeyManagementException | NoSuchAlgorithmException e) {
					g().errorLog.add(e);
				}
			}
		}, "check new version thread");// .start();
	}

	public Version lastVersionOnline() throws MalformedURLException, IOException, NoSuchAlgorithmException, KeyManagementException {
		var v = new Version();
		System.out.println(lastVersionURL);
		// Before calling URL.openStream() at line 57:
		TrustManager[] trustAllCerts = new TrustManager[]{
		    new X509TrustManager() {
		        public X509Certificate[] getAcceptedIssuers() { return null; }
		        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
		        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
		    }
		};

		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		// Optional: Bypass hostname verification if the cert belongs to a different domain variant
		HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
		var jsonString = new String(new URL(lastVersionURL).openStream().readAllBytes());
		JsonNode rootNode = objectMapper.readTree(jsonString);
		v.set(rootNode.get("version").asText());
		return v;
	}

	@Override
	public void createActions() {
		cachedActions.elements.add(new Update(this));
		cachedActions.elements.add(new Deploy(this));
		super.createActions();
	}

	@Override
	public String toString() {
		return "Byransha";
	}

	@Override
	public String whatIsThis() {
		return "Byransha";
	}
}
