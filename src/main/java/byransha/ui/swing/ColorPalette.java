package byransha.ui.swing;

import java.awt.Color;

public class ColorPalette {

	public enum Style {
		ASSORTED, NEON, EARTHY, MONOCHROME, RETRO
	}

	// --- WCAG contrast utilities ---

	// Relative luminance per WCAG 2.1
	public static double luminance(Color c) {
		return 0.2126 * linearize(c.getRed() / 255.0)
				+ 0.7152 * linearize(c.getGreen() / 255.0)
				+ 0.0722 * linearize(c.getBlue() / 255.0);
	}

	private static double linearize(double v) {
		return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
	}

	// Contrast ratio between two colors per WCAG 2.1
	public static double contrastRatio(Color c1, Color c2) {
		double l1 = luminance(c1), l2 = luminance(c2);
		double lighter = Math.max(l1, l2), darker = Math.min(l1, l2);
		return (lighter + 0.05) / (darker + 0.05);
	}

	// All colors in all palettes guarantee contrast >= 4.5 against white (WCAG AA)
	public static boolean isReadableOnWhiteText(Color bg) {
		return contrastRatio(bg, Color.WHITE) >= 4.5;
	}

	// --- ASSORTED: broad spectrum, vivid ---
	public static final Color[] ASSORTED = {
			new Color(0xC0392B), new Color(0xC44033), new Color(0xB34700), new Color(0xA55A17),
			new Color(0x945F0A), new Color(0x826907), new Color(0x1E8449), new Color(0x1C7C44),
			new Color(0x117965), new Color(0x0F725F), new Color(0x1A5276), new Color(0x226C9D),
			new Color(0x6C3483), new Color(0x8E44AD), new Color(0x2C3E50), new Color(0xA93226),
			new Color(0x784212), new Color(0x1D6A39), new Color(0x0E6655), new Color(0x1A4F72),
			new Color(0x512E5F), new Color(0x1C2833), new Color(0x641E16), new Color(0x4D1A05),
			new Color(0x145A32), new Color(0x0B4619), new Color(0x154360), new Color(0x4A235A),
			new Color(0x212F3C), new Color(0x7B241C), new Color(0x6E2C00), new Color(0x1E8449),
	};

	// --- NEON: high saturation, dark enough for white text ---
	public static final Color[] NEON = {
			new Color(0xD80039), new Color(0xD82B56), new Color(0xD80082), new Color(0xB72493),
			new Color(0xAD00D8), new Color(0x9900FF), new Color(0x6600FF), new Color(0x3300FF),
			new Color(0x0033FF), new Color(0x006EB7), new Color(0xB74900), new Color(0xD82B00),
			new Color(0xCC0000), new Color(0x990099), new Color(0x660099), new Color(0x003399),
			new Color(0x006699), new Color(0x008256), new Color(0x006600), new Color(0x996600),
			new Color(0xCC3300), new Color(0xCC0066), new Color(0x9900CC), new Color(0x6600CC),
			new Color(0x0000CC), new Color(0x0066CC), new Color(0x007A99), new Color(0x007A4D),
			new Color(0x805500), new Color(0xAA2200), new Color(0xAA0055), new Color(0x550088),
	};

	// --- EARTHY: warm, muted, natural tones ---
	public static final Color[] EARTHY = {
			new Color(0x3B1F0E), new Color(0x5C2E0A), new Color(0x7B3F00), new Color(0x8B4513),
			new Color(0xA0522D), new Color(0xA65D1F), new Color(0xB25919), new Color(0x93602D),
			new Color(0x4A5240), new Color(0x5C6B4E), new Color(0x6B7C54), new Color(0x636E4C),
			new Color(0x3D3535), new Color(0x5C4A3A), new Color(0x7A6552), new Color(0x6B6B4A),
			new Color(0x76764C), new Color(0x4E3524), new Color(0x6B3A2A), new Color(0x523A28),
			new Color(0x3C2F1E), new Color(0x2E2416), new Color(0x4A3728), new Color(0x5E4A38),
			new Color(0x3A4A30), new Color(0x2E3A26), new Color(0x243020), new Color(0x504530),
			new Color(0x6A5040), new Color(0x584038), new Color(0x483530), new Color(0x705848),
	};

	// --- MONOCHROME: dark greys only ---
	public static final Color[] MONOCHROME = {
			new Color(0x000000), new Color(0x080808), new Color(0x111111), new Color(0x1A1A1A),
			new Color(0x222222), new Color(0x2E2E2E), new Color(0x3A3A3A), new Color(0x484848),
			new Color(0x555555), new Color(0x636363), new Color(0x707070), new Color(0x6B6B6B),
			new Color(0x242424), new Color(0x303030), new Color(0x3C3C3C), new Color(0x404040),
			new Color(0x4C4C4C), new Color(0x505050), new Color(0x585858), new Color(0x606060),
			new Color(0x686868), new Color(0x6C6C6C), new Color(0x747474), new Color(0x666666),
			new Color(0x1C1C1C), new Color(0x282828), new Color(0x343434), new Color(0x444444),
			new Color(0x5C5C5C), new Color(0x646464), new Color(0x6E6E6E), new Color(0x727272),
	};

	// --- RETRO: muted 70s/80s inspired tones ---
	public static final Color[] RETRO = {
			new Color(0xC0392B), new Color(0xC54923), new Color(0x995E4C), new Color(0x377D59),
			new Color(0x217166), new Color(0x277191), new Color(0x3A5A8C), new Color(0x5B4F9E),
			new Color(0x8B4FAE), new Color(0xBD4F8C), new Color(0xAA435A), new Color(0x736653),
			new Color(0x6B5C4A), new Color(0x4E4035), new Color(0x77685D), new Color(0x5D7760),
			new Color(0x4E6E52), new Color(0x365040), new Color(0x67775D), new Color(0xA0522D),
			new Color(0x7B3F00), new Color(0x5C4033), new Color(0x8B4513), new Color(0x6A3728),
			new Color(0x2C5F6A), new Color(0x1D4E5A), new Color(0x3A4A6A), new Color(0x5A3A6A),
			new Color(0x6A3A5A), new Color(0x5A3A3A), new Color(0x3A5A4A), new Color(0x4A5A3A),
	};

	public static Color[] forStyle(Style style) {
		return switch (style) {
			case ASSORTED -> ASSORTED;
			case NEON -> NEON;
			case EARTHY -> EARTHY;
			case MONOCHROME -> MONOCHROME;
			case RETRO -> RETRO;
		};
	}

	public static Color forClass(Class<?> clazz, Style style) {
		Color[] palette = forStyle(style);
		return palette[Math.abs(clazz.getName().hashCode()) % palette.length];
	}

	public static Color forClass(Class<?> clazz) {
		return forClass(clazz, Style.ASSORTED);
	}

	public static void main(String[] args) {
		for (Style style : Style.values()) {
			Color[] palette = forStyle(style);
			System.out.printf("%n--- %s ---%n", style);
			for (Color c : palette) {
				double ratio = contrastRatio(c, Color.WHITE);
				System.out.printf("#%02X%02X%02X  contrast: %.2f  %s%n",
						c.getRed(), c.getGreen(), c.getBlue(),
						ratio, ratio >= 4.5 ? "✓" : "✗");
			}
		}
	}
}