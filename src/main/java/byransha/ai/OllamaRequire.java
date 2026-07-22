package byransha.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import byransha.ai.OllamaRequire.OS;

public class OllamaRequire {

	public enum OS {
		WINDOWS, MACOS, LINUX, UNKNOWN
	}

	private static OS currentOS = detectOS();
	private static Scanner scanner = new Scanner(System.in);

	public static boolean checkRequirements() {
		System.out.println(" *** Checking requirements *** ");
		List<String> missing = new ArrayList<>();
		checkTool("ollama", "--version", "Ollama", missing);

		if (!missing.isEmpty()) {
			System.out.println("***");
			System.out.println("  " + missing.size() + " outil(s) manquant(s): " + String.join(", ", missing));
			System.out.println("***");

			for (String tool : missing) {
				switch (tool.toLowerCase()) {
					case "ollama":
						if (currentOS == OS.WINDOWS) {
							installTool("Ollama", "powershell -c \"irm https://ollama.com/install.ps1 | iex\"");
						} else {
							installTool("Ollama", "curl -fsSL https://ollama.com/install.sh | sh");
						}
						System.out.println("***\nOllama - outil de gestion de modèles d'IA locaux\n***");
						break;
				}
			}

			System.out.println("***");
			System.out.println(" Veuillez relancer l'application après avoir installé les outils manquants.");
			System.out
					.println(" N'oubliez pas de recharger votre terminal pour mettre à jour votre PATH si nécessaire.");
			System.out.println("***");
			System.exit(1);
		}

		System.out.println("***\n All requirements are met!\n");

		// On s'assure que le modèle est téléchargé ("pull" plutôt que "run" pour éviter
		// que le chat bloque l'appli JVM)
		downloadOllamaModel("granite4:tiny-h");
		return true;
	}

	private static void downloadOllamaModel(String modelName) {
		System.out.println("***\nChecking/Downloading Ollama model: " + modelName + "...");
		try {
			ProcessBuilder pb;
			if (currentOS == OS.WINDOWS) {
				pb = new ProcessBuilder("cmd.exe", "/c", "ollama pull " + modelName);
			} else {
				pb = new ProcessBuilder("sh", "-c", "ollama pull " + modelName);
			}
			pb.inheritIO();
			Process process = pb.start();
			process.waitFor();

			if (process.exitValue() == 0) {
				System.out.println(" Model " + modelName + " is ready.");
			} else {
				System.out.println(" Error downloading model " + modelName);
			}
		} catch (Exception e) {
			System.out.println(" Error executing ollama command: " + e.getMessage());
		}
	}

	private static void checkTool(String command, String versionArgs, String displayName, List<String> missing) {
		if (isCommandAvailable(command, versionArgs)) {
			System.out.println(" " + displayName + " is installed");
		} else {
			System.out.println(" " + displayName + " is not installed");
			missing.add(displayName);
		}
	}

	private static boolean isCommandAvailable(String command, String args) {
		try {
			ProcessBuilder pb;
			if (currentOS == OS.WINDOWS) {
				pb = new ProcessBuilder("cmd.exe", "/c", command + " " + args);
			} else {
				pb = new ProcessBuilder("sh", "-c", command + " " + args);
			}
			Process process = pb.start();
			process.waitFor();
			return process.exitValue() == 0;
		} catch (Exception e) {
			return false;
		}
	}

	private static void installTool(String toolName, String installCmd) {
		System.out.print("Do you want to download " + toolName + " now? (y/n): ");
		String answer = scanner.nextLine().trim().toLowerCase();

		if (answer.equals("y") || answer.equals("yes")) {
			System.out.println("***\nInstalling " + toolName + "...");
			try {
				ProcessBuilder pb;
				if (currentOS == OS.WINDOWS) {
					pb = new ProcessBuilder("cmd.exe", "/c", installCmd);
				} else {
					pb = new ProcessBuilder("sh", "-c", installCmd);
				}
				pb.inheritIO(); // Affiche la sortie directement dans la console de l'app Java
				Process process = pb.start();
				process.waitFor();

				if (process.exitValue() == 0) {
					System.out.println(" " + toolName + " installed successfully");
				} else {
					System.out.println(" Error installing " + toolName);
				}
			} catch (Exception e) {
				System.out.println(" Error executing installation command: " + e.getMessage());
			}
		} else {
			System.out.println("Installation of " + toolName + " ignored by user.");
		}
	}

	private static OS detectOS() {
		String os = System.getProperty("os.name").toLowerCase();
		System.out.println("Detected OS: " + os);
		if (os.contains("win")) {
			return OS.WINDOWS;
		} else if (os.contains("mac")) {
			return OS.MACOS;
		} else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
			return OS.LINUX;
		}
		return OS.UNKNOWN;
	}

	public static void main(String[] args) {
		checkRequirements();
	}
}
