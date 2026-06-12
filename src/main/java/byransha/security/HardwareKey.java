package byransha.security;

import java.net.NetworkInterface;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class HardwareKey {

	private static final String ALGORITHM = "AES";
	private static final String KDF = "PBKDF2WithHmacSHA256";
	private static final int ITERATIONS = 310_000;
	private static final int KEY_SIZE = 256;

	// Fixed salt — bake into your application at build time
	// Change this per-application to namespace your keys
	// TODO: avoid hardcoding the salt.
	private static final byte[] APP_SALT = { (byte) 0xA3, (byte) 0x7F, (byte) 0x2C, (byte) 0x88, (byte) 0x14,
			(byte) 0xE6, (byte) 0x5B, (byte) 0x91, (byte) 0xD2, (byte) 0x4A, (byte) 0x09, (byte) 0xCC, (byte) 0x73,
			(byte) 0x1E, (byte) 0xF0, (byte) 0x3D };

	/*
	 * FIX: Maybe consider changing how the fingerprint is done. As it is now,
	 * an update can change it and the encrypion key would be permanently lost.
	 * TODO: Consider using Java KeyStore (JKS).
	 */

	/**
	 * Derives a stable AES-256 key from local hardware components. The same
	 * hardware will always produce the same key.
	 */
	public static SecretKey derive() throws Exception {
		String fingerprint = collectFingerprint();
		return deriveKey(fingerprint);
	}

	// --- Fingerprint collection ---

	private static String collectFingerprint() {
		List<String> components = new ArrayList<>();

		safeAdd(components, getMacAddresses());
		safeAdd(components, getCpuInfo());
		safeAdd(components, getMotherboardSerial());
		safeAdd(components, getOsInfo());
		safeAdd(components, getDiskSerial());

		if (components.isEmpty()) {
			throw new IllegalStateException("Could not collect any hardware fingerprint components");
		}

		// Sort for stability — order of network interfaces etc. can vary
		Collections.sort(components);
		return String.join("|", components);
	}

	private static void safeAdd(List<String> list, String value) {
		if (value != null && !value.isBlank())
			list.add(value);
	}

	// MAC addresses of all non-loopback, non-virtual interfaces
	private static String getMacAddresses() {
		try {
			StringBuilder sb = new StringBuilder();
			Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
			List<String> macs = new ArrayList<>();
			while (ifaces.hasMoreElements()) {
				NetworkInterface iface = ifaces.nextElement();
				if (iface.isLoopback() || iface.isVirtual())
					continue;
				byte[] mac = iface.getHardwareAddress();
				if (mac == null)
					continue;
				StringBuilder macStr = new StringBuilder();
				for (byte b : mac)
					macStr.append(String.format("%02X", b));
				macs.add(macStr.toString());
			}
			Collections.sort(macs);
			return "MAC:" + String.join(",", macs);
		} catch (Exception e) {
			return null;
		}
	}

	// CPU info from /proc/cpuinfo (Linux) or registry (Windows)
	private static String getCpuInfo() {
		try {
			String os = System.getProperty("os.name").toLowerCase();
			if (os.contains("linux")) {
				return "CPU:" + Files.lines(Path.of("/proc/cpuinfo"))
						.filter(l -> l.startsWith("model name") || l.startsWith("cpu MHz") || l.startsWith("processor"))
						.findFirst().orElse("").replaceAll("\\s+", " ").trim();
			} else if (os.contains("win")) {
				Process p = Runtime.getRuntime().exec("wmic cpu get Name,ProcessorId /value");
				String out = new String(p.getInputStream().readAllBytes());
				return "CPU:" + out.replaceAll("\\s+", "").trim();
			} else if (os.contains("mac")) {
				Process p = Runtime.getRuntime().exec(new String[] { "sysctl", "-n", "machdep.cpu.brand_string" });
				return "CPU:" + new String(p.getInputStream().readAllBytes()).trim();
			}
		} catch (Exception e) {
			/* ignore */ }
		// Fallback to available processors count
		return "CPU_CORES:" + Runtime.getRuntime().availableProcessors();
	}

	// Motherboard/system serial number
	// TODO: requires root. Maybe it would be better to change to
	// /etc/machine-id instead ?
	private static String getMotherboardSerial() {
		try {
			String os = System.getProperty("os.name").toLowerCase();
			if (os.contains("linux")) {
				// Requires root or appropriate permissions
				Path p = Path.of("/sys/class/dmi/id/board_serial");
				if (Files.exists(p))
					return "MB:" + Files.readString(p).trim();
				p = Path.of("/sys/class/dmi/id/product_uuid");
				if (Files.exists(p))
					return "UUID:" + Files.readString(p).trim();
			} else if (os.contains("win")) {
				Process p = Runtime.getRuntime().exec("wmic baseboard get SerialNumber /value");
				String out = new String(p.getInputStream().readAllBytes());
				return "MB:" + out.replaceAll("\\s+", "").trim();
			} else if (os.contains("mac")) {
				Process p = Runtime.getRuntime().exec(new String[] { "ioreg", "-l" });
				return "MB:" + new String(p.getInputStream().readAllBytes()).lines()
						.filter(l -> l.contains("IOPlatformSerialNumber")).findFirst().orElse("").trim();
			}
		} catch (Exception e) {
			/* ignore */ }
		return null;
	}

	// Disk serial (Linux/Windows/Mac)
	private static String getDiskSerial() {
		try {
			String os = System.getProperty("os.name").toLowerCase();
			if (os.contains("linux")) {
				Process p = Runtime.getRuntime()
						.exec(new String[] { "sh", "-c", "lsblk -d -o serial 2>/dev/null | tail -1" });
				String out = new String(p.getInputStream().readAllBytes()).trim();
				if (!out.isBlank())
					return "DISK:" + out;
			} else if (os.contains("win")) {
				Process p = Runtime.getRuntime().exec("wmic diskdrive get SerialNumber /value");
				String out = new String(p.getInputStream().readAllBytes());
				return "DISK:" + out.replaceAll("\\s+", "").trim();
			} else if (os.contains("mac")) {
				Process p = Runtime.getRuntime()
						.exec(new String[] { "sh", "-c", "diskutil info disk0 | grep 'Disk / Partition UUID'" });
				return "DISK:" + new String(p.getInputStream().readAllBytes()).trim();
			}
		} catch (Exception e) {
			/* ignore */ }
		return null;
	}

	// OS info — stable across reboots
	private static String getOsInfo() {
		return "OS:" + System.getProperty("os.name") + ":" + System.getProperty("os.arch");
	}

	// --- Key derivation ---

	private static SecretKey deriveKey(String fingerprint) throws Exception {
		// Hash the fingerprint first for uniform length input
		MessageDigest sha = MessageDigest.getInstance("SHA-256");
		byte[] fingerprintHash = sha.digest(fingerprint.getBytes("UTF-8"));
		String password = HexFormat.of().formatHex(fingerprintHash);

		// Derive key using PBKDF2
		SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF);
		KeySpec spec = new PBEKeySpec(password.toCharArray(), APP_SALT, ITERATIONS, KEY_SIZE);
		return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), ALGORITHM);
	}

	// --- Utilities ---

	/**
	 * Returns the raw fingerprint string for debugging. Never log this in
	 * production.
	 */
	public static String fingerprintDebug() throws Exception {
		return collectFingerprint();
	}

	/**
	 * Returns a stable hex identifier for this machine (SHA-256 of fingerprint,
	 * safe to log/store).
	 */
	public static String machineId() throws Exception {
		MessageDigest sha = MessageDigest.getInstance("SHA-256");
		return HexFormat.of().formatHex(sha.digest(collectFingerprint().getBytes("UTF-8")));
	}

	public static void main(String[] args) throws Exception {
		System.out.println("Fingerprint: " + fingerprintDebug());
		System.out.println("Machine ID:  " + machineId());
		SecretKey key = derive();
		System.out.println("Key (hex):   " + HexFormat.of().formatHex(key.getEncoded()));
	}
}
