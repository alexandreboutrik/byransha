package byransha.util;

public enum Stop {
	no, yes;

	public static Stop stopIf(boolean b) {
		return b ? yes : no;
	}

}