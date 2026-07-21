package byransha.util;

public class Base62 {

	private static final String CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

	private static final long UNSIGNED_MASK = 0xFFFFFFFFFFFFFFFFL;

	public static String encode(long value) {
		if (value == 0)
			return "0";

		// Treat the bits as unsigned, like hex does
		long unsigned = value;
		StringBuilder sb = new StringBuilder();

		while (Long.compareUnsigned(unsigned, 0) != 0) {
			long rem = Long.remainderUnsigned(unsigned, 62);
			sb.append(CHARS.charAt((int) rem));
			unsigned = Long.divideUnsigned(unsigned, 62);
		}

		return sb.reverse().toString();
	}

	public static long decode(String s) {
		if (s == null || s.isEmpty())
			throw new IllegalArgumentException("Empty input");

		long result = 0;
		for (int i = 0; i < s.length(); i++) {
			int digit = CHARS.indexOf(s.charAt(i));
			if (digit == -1)
				throw new IllegalArgumentException("Invalid character: " + s.charAt(i));
			result = result * 62 + digit;
		}
		return result;
	}

	public static void main(String[] args) {
		long[] tests = { 0, 1, 62, 12345, -1, -12345, Long.MAX_VALUE, Long.MIN_VALUE };
		for (long v : tests) {
			String enc = encode(v);
			System.out.printf("%20d  →  %-14s  →  %d  (hex: %s)%n",
					v, enc, decode(enc), Long.toHexString(v));
		}
	}
}