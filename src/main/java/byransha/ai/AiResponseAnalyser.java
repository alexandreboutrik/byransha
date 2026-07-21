package byransha.ai;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AiResponseAnalyser {
	private static final ObjectMapper mapper = new ObjectMapper();

	/* Extracts the first valid JSON payload from the given text. */
	public static String extractFirstJsonPayload(String responseText) {
		if (responseText == null) {
			return null;
		}
		String trimmed = responseText.trim();
		if (isValidJson(trimmed)) {
			return trimmed;
		}

		String bestPayload = null;
		int bestScore = Integer.MIN_VALUE;

		for (int i = 0; i < responseText.length(); i++) {
			char c = responseText.charAt(i);
			if (c != '{' && c != '[') {
				continue;
			}

			String candidate = extractBalancedPayload(responseText, i);
			if (candidate == null) {
				continue;
			}

			String[] variants = new String[] { candidate, normalizePythonLikeJson(candidate) };

			for (String variant : variants) {
				if (variant == null || !isValidJson(variant)) {
					continue;
				}

				int score = scorePayload(variant, i, responseText.length());
				if (score > bestScore) {
					bestScore = score;
					bestPayload = variant;
				}
			}
		}

		return bestPayload;
	}

	/*
	 * Extracts a balanced JSON object or array from the given text starting at the
	 * specified index.
	 */

	private static String extractBalancedPayload(String text, int start) {
		char opening = text.charAt(start);
		if (opening != '{' && opening != '[') {
			return null;
		}

		char closing = opening == '{' ? '}' : ']';
		int depth = 0;
		char stringDelimiter = 0;
		boolean escaped = false;

		for (int i = start; i < text.length(); i++) {
			char c = text.charAt(i);

			if (stringDelimiter != 0) {
				if (escaped) {
					escaped = false;
				} else if (c == '\\') {
					escaped = true;
				} else if (c == stringDelimiter) {
					stringDelimiter = 0;
				}
				continue;
			}

			if (c == '"' || c == '\'') {
				stringDelimiter = c;
				continue;
			}

			if (c == opening) {
				depth++;
			} else if (c == closing) {
				depth--;
				if (depth == 0) {
					return text.substring(start, i + 1).trim();
				}
			}
		}

		return null;
	}

	private static String normalizePythonLikeJson(String candidate) {
		if (candidate == null || candidate.isBlank()) {
			return null;
		}

		String normalized = normalizeSingleQuotedStrings(candidate);
		normalized = normalized.replaceAll("\\bNone\\b", "null");
		normalized = normalized.replaceAll("\\bTrue\\b", "true");
		normalized = normalized.replaceAll("\\bFalse\\b", "false");
		return normalized;
	}

	private static String normalizeSingleQuotedStrings(String input) {
		StringBuilder out = new StringBuilder(input.length());
		boolean inSingle = false;
		boolean inDouble = false;
		boolean escaped = false;

		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);

			if (inSingle) {
				if (escaped) {
					out.append(c);
					escaped = false;
					continue;
				}

				if (c == '\\') {
					out.append(c);
					escaped = true;
					continue;
				}

				if (c == '\'') {
					out.append('"');
					inSingle = false;
					continue;
				}

				if (c == '"') {
					out.append("\\\\\"");
				} else {
					out.append(c);
				}
				continue;
			}

			if (inDouble) {
				out.append(c);
				if (escaped) {
					escaped = false;
				} else if (c == '\\') {
					escaped = true;
				} else if (c == '"') {
					inDouble = false;
				}
				continue;
			}

			if (c == '\'') {
				out.append('"');
				inSingle = true;
			} else {
				out.append(c);
				if (c == '"') {
					inDouble = true;
				}
			}
		}

		return out.toString();
	}

	private static int scorePayload(String payload, int start, int totalLength) {
		int score = 0;

		if (isDistribution(payload)) {
			score += 100;
		} else if (isArrayOfNumbers(payload)) {
			score += 90;
		} else {
			try {
				JsonNode node = mapper.readTree(payload);
				if (node.isObject() || node.isArray()) {
					score += node.isEmpty() ? 10 : 50;
				}
			} catch (JsonProcessingException e) {
				return Integer.MIN_VALUE;
			}
		}

		score += (start * 10) / Math.max(1, totalLength);
		return score;
	}

	private static boolean isValidJson(String jsonString) {
		try {
			mapper.readTree(jsonString);
			return true;
		} catch (JsonProcessingException e) {
			return false;
		}
	}

	/**
	 * Vérifie si la chaîne JSON est un tableau contenant uniquement des nombres.
	 */
	public static boolean isArrayOfNumbers(String jsonString) {
		try {
			JsonNode rootNode = mapper.readTree(jsonString);
			if (!rootNode.isArray()) {
				return false;
			}
			for (JsonNode node : rootNode) {
				if (!node.isNumber()) {
					return false;
				}
			}
			return true;
		} catch (JsonProcessingException e) {
			// pas un JSON valide
			return false;
		}
	}

	/**
	 * Vérifie si la chaîne JSON décrit une distribution (un dictionnaire String ->
	 * Nombre).
	 */
	public static boolean isDistribution(String jsonString) {
		try {
			JsonNode rootNode = mapper.readTree(jsonString);
			if (!rootNode.isObject()) {
				return false;
			}

			boolean hasEntry = false;
			for (Map.Entry<String, JsonNode> entry : rootNode.properties()) {
				hasEntry = true;
				JsonNode value = entry.getValue();
				if (!value.isNumber()) {
					return false;
				}
			}
			return hasEntry;
		} catch (JsonProcessingException e) {
			// pas JSON valide
			return false;
		}
	}
}
