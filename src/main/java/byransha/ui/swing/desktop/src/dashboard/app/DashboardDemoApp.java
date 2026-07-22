package byransha.ui.swing.desktop.src.dashboard.app;

import javax.swing.*;
import byransha.ui.swing.desktop.src.dashboard.core.engine.DashboardLayout;
import byransha.ui.swing.desktop.src.dashboard.core.persistence.DashboardLoader;
import byransha.ui.swing.desktop.src.dashboard.core.persistence.DashboardSaver;
import byransha.ui.swing.desktop.src.dashboard.swing.DashboardController;
import byransha.ui.swing.desktop.src.dashboard.swing.DashboardViewBuilder;

public class DashboardDemoApp {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			DashboardLoader loader = new DashboardLoader();
			DashboardLayout layout = loader.loadFromFile("src/main/java/byransha/ui/swing/desktop/save.json");

			DashboardViewBuilder builder = new DashboardViewBuilder();
			DashboardSaver saver = new DashboardSaver();

			JFrame frame = new JFrame("Dashboard Framework Demo");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setSize(800, 600);

			DashboardController controller = new DashboardController(layout, builder, saver, frame);
			controller.refreshUI();

			frame.setVisible(true);
		});
	}
}
