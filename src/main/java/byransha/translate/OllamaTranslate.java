package byransha.translate;

import byransha.graph.BGraph;
import byransha.translate.Translator.Language;
import dev.langchain4j.model.ollama.OllamaChatModel;

public class OllamaTranslate extends Translator {

    private static final OllamaChatModel chatModel = OllamaChatModel.builder()
            .baseUrl("http://localhost:11434")
            .modelName("granite4:tiny-h")
            .temperature(0.2)
            .build();

	public OllamaTranslate(BGraph g) {
		super(g);
	}

	@Override
	public String googleTranslate(String text, Language from, Language to) throws Exception {
		return chatModel.generate("please translate the following text from " + from + " to " + to + ":\n\n" + text);
	}

	@Override
	public String toString() {
		return "ollama";
	}
}
