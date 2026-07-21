package byransha.translate;

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import byransha.graph.BGraph;
import byransha.graph.BNode;
import byransha.graph.ShowInKishanView;
import byransha.nodes.primitive.StringNode;
import byransha.ui.swing.ComponentShowingTextAndToolTip;

public abstract class Translator extends BNode {
	public enum Language {
		fr, en, auto, es, it, de, lu
	};

	@ShowInKishanView
	final File translateDir;

	@ShowInKishanView
	public final StringNode targetLanguage;
	List<Dictionary> dictionaries = new ArrayList<>();

	public Translator(BGraph g) {
		super(g);
		new Thread(() -> {
			while (true) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					g().errorLog.add(e);
				}

				dictionaries.stream().filter(d -> d.needSave).forEach(d -> {
					try {
						synchronized (this) {
							d.save();
						}
					} catch (IOException err) {
						g().errorLog.add(err);
					}
				});
			}
		}).start();

		translateDir = new File(g.byransha.homeDirectory, "translate");
		translateDir.mkdirs();

		targetLanguage = new StringNode(g, "en", ".+");

		targetLanguage.valueChangeListeners.add((n, a, b) -> {
			if (userDefinedTargetLanguage() != null) {
				new Thread(() -> translateRecursively(g.swing.f.getContentPane(), new HashSet<>())).start();
			}
		});
	}

	private void translateRecursively(Component c, Set<Component> visited) {
		visited.add(c);

		if (c instanceof ComponentShowingTextAndToolTip tr) {
			String initialText = tr.getText();
			var translated = translate(initialText);

			if (translated != null) {
				tr.setText(translated);

				if (c instanceof JComponent jc) {
					SwingUtilities.invokeLater(() -> {
						Rectangle rect = new Rectangle(0, 0, c.getWidth(), c.getHeight());
						jc.scrollRectToVisible(rect);
					});
				}
			}
		} else if (c instanceof Container l) {
			for (int i = 0; i < l.getComponentCount(); ++i) {
				var child = l.getComponent(i);

				if (!visited.contains(child)) {
					translateRecursively(child, visited);
				}
			}
		}
	}

	Language userDefinedTargetLanguage() {
		try {
			return Language.valueOf(targetLanguage.get());
		} catch (Throwable err) {
			return null;
		}
	}

	public String translate(String s) {
		return translate(s, Language.en, userDefinedTargetLanguage());
	}

	public String translate(String s, Language to) {
		return translate(s, Language.auto, to);
	}

	public synchronized String translate(final String originalText, Language from, Language to) {
		if (from == to)
			return originalText;

		if (originalText.isBlank() || originalText.matches("[0-9]+"))
			return null;

		var dictionary = dictionaries.stream().filter(d -> d.from == from && d.to == to).findFirst()
				.orElseGet(() -> null);

		if (dictionary == null) {
			try {
				var file = new File(translateDir, from + "-" + to + ".json");
				dictionary = new Dictionary(from, to, file);
				dictionaries.add(dictionary);
			} catch (IOException err) {
				g().errorLog.add(err);
				return null;
			}
		}

		var translated = dictionary.lookup(originalText);

		try {
			translated = googleTranslate(originalText, from, to);
			System.out.println("original text:  " + originalText);
			System.out.println("translated text:  " + translated);

			if (!originalText.equals(translated)) {
				dictionary.put(originalText, translated);
				return translated;
			} else {
				return null;
			}

		} catch (Exception e) {
			g().errorLog.add(e);
			return null;
		}
	}

	public abstract String googleTranslate(String text, Language from, Language to) throws Exception;

	@Override
	public String whatIsThis() {
		return "translator";
	}

	@Override
	public String toString() {
		return "Google translate";
	}
}
