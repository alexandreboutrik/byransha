package byransha;

import java.io.PrintWriter;
import java.util.function.BiConsumer;

public class Logger implements BiConsumer<LogType, String> {
	public static final Logger log = new Logger();

	PrintWriter writer = new PrintWriter(System.out, true);

	@Override
	public void accept(LogType logtype, String s) {
		writer.println(logtype.name() + "\t" + s);

	}
}
