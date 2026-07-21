package byransha.nodes.system;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import com.fasterxml.jackson.databind.node.ObjectNode;

import byransha.Main;
import byransha.graph.Action;
import byransha.graph.ShowInKishanView;
import byransha.nodes.primitive.StringNode;
import byransha.nodes.system.Update.byransha;
import byransha.util.MinaScpUploader;
import byransha.util.Version.Level;

public class Deploy extends Action<Byransha> {
	@ShowInKishanView
	public final StringNode scpHost = new StringNode(this, "bastion.i3s.unice.fr", ".+");
	@ShowInKishanView
	public final StringNode scpRemoteDir = new StringNode(this, "public_html/software/byransha/downloads/bin/", ".+");
	@ShowInKishanView
	public final StringNode username = new StringNode(this, "hogie", ".+");
	@ShowInKishanView
	public final StringNode version = new StringNode(this, "", ".+");

	public Deploy(Byransha b) {
		super(b, byransha.class);
		var versionNode = ((Byransha) parent).versionNode;
		versionNode.version.upgrade(Level.revision);
		version.set(versionNode.version.toString());
		hasButtonOnKishanView = true;
	}

	@Override
	public String whatItDoes() {
		return "deploy the local version";
	}

	@Override
	protected void impl() throws Throwable {
		File outputJar = File.createTempFile(getClass().getName(), ".jar");

		var versionFile = File.createTempFile(getClass().getName(), ".txt");
		var n = new ObjectNode(factory);
		n.put("version", version.get());
		n.put("date", LocalDateTime.now().toString());
		n.put("java.version", System.getProperty("java.specification.version"));
		Files.writeString(versionFile.toPath(), n.toPrettyString());
		scp(versionFile, scpHost.get(), scpRemoteDir.get() + "/info.json", username.get(), null);

		var installFile = File.createTempFile(getClass().getName(), "ps1");
		Files.write(installFile.toPath(), getClass().getResourceAsStream("run.ps1").readAllBytes());
		scp(installFile, scpHost.get(), scpRemoteDir.get() + "/run.ps1", username.get(), null);

		JarFlattener.flattenClasspathToJar(outputJar);
		scp(outputJar, scpHost.get(), scpRemoteDir.get() + "/byransha.jar", username.get(), null);
	}

	private void scp(File f, String host, String remoteDir, String username, String password) throws IOException {
		MinaScpUploader.uploadWithPrivateKey(host, 22, username, System.getProperty("user.home") + "/.ssh/id_rsa", null,
				f.getAbsolutePath(), remoteDir);
	}

	public class JarFlattener {

		/**
		 * Flattens the current system classpath into a single large uber-jar. * @param
		 * outputJar The destination path for the consolidated fat jar.
		 *
		 * @throws IOException
		 *                         If file reading or writing fails.
		 */
		public static void flattenClasspathToJar(File outputJar) throws IOException {
			// 1. Get the current classpath string split by the OS path separator
			String classpath = System.getProperty("java.class.path");
			String[] classpathElements = classpath.split(File.pathSeparator);

			// Track written entries to avoid DuplicateEntryException crashes
			Set<String> processedEntries = new HashSet<>();

			// Create a basic Manifest for our new consolidated archive
			Manifest manifest = new Manifest();
			manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
			manifest.getMainAttributes().put(new Attributes.Name("Created-By"), "Java JarFlattener");
			manifest.getMainAttributes().put(new Attributes.Name("Main-Class"), Main.class.getName());

			System.out.println("Creating unified archive at: " + outputJar.getAbsolutePath());

			try (JarOutputStream jos = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(outputJar)),
					manifest)) {

				for (String element : classpathElements) {
					if (element == null || element.isEmpty())
						continue;

					File cpFile = new File(element);
					if (!cpFile.exists())
						continue;

					if (cpFile.isFile() && cpFile.getName().endsWith(".jar")) {
						// Avoid swallowing our own tail if the output file is already on the classpath
						if (cpFile.getAbsoluteFile().equals(outputJar.getAbsoluteFile()))
							continue;

						System.out.println("Flattening dependency: " + cpFile.getName());
						flattenJarElement(cpFile, jos, processedEntries);
					} else if (cpFile.isDirectory()) {
						System.out.println("Packing class directory: " + cpFile.getName());
						flattenDirectoryElement(cpFile.toPath(), cpFile.toPath(), jos, processedEntries);
					}
				}
			}
			System.out.println("Flattening complete! Total unique entries packed: " + processedEntries.size());
		}

		private static void flattenJarElement(File jarFile, JarOutputStream jos, Set<String> processedEntries)
				throws IOException {
			try (JarFile jar = new JarFile(jarFile)) {
				var entries = jar.entries();
				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					String name = entry.getName();

					// Skip manifests, signatures, and already processed entry frames
					if (shouldSkip(name) || !processedEntries.add(name)) {
						continue;
					}

					// Create a clean entry frame without compression metadata conflicts
					JarEntry newEntry = new JarEntry(name);
					jos.putNextEntry(newEntry);

					try (InputStream is = jar.getInputStream(entry)) {
						is.transferTo(jos); // Efficient stream-to-stream pumping (Java 9+)
					}
					jos.closeEntry();
				}
			}
		}

		private static void flattenDirectoryElement(Path rootDir, Path currentPath, JarOutputStream jos,
				Set<String> processedEntries) throws IOException {
			// Walk local file directories (like target/classes/ during development)
			try (var stream = Files.newDirectoryStream(currentPath)) {
				for (Path path : stream) {
					if (Files.isDirectory(path)) {
						flattenDirectoryElement(rootDir, path, jos, processedEntries);
					} else {
						// Create relative zip paths (e.g., com/app/Main.class)
						String name = rootDir.relativize(path).toString().replace('\\', '/');

						if (shouldSkip(name) || !processedEntries.add(name)) {
							continue;
						}

						JarEntry entry = new JarEntry(name);
						jos.putNextEntry(entry);
						Files.copy(path, jos);
						jos.closeEntry();
					}
				}
			}
		}

		private static boolean shouldSkip(String entryName) {
			return entryName.equalsIgnoreCase("META-INF/MANIFEST.MF") || entryName.startsWith("META-INF/SIG-")
					|| entryName.endsWith(".SF") || entryName.endsWith(".DSA") || entryName.endsWith(".RSA");
		}
	}

	@Override
	public boolean applies() {
		return true;
	}

}
