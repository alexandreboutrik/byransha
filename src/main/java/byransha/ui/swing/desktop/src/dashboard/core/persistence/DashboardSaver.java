package byransha.ui.swing.desktop.src.dashboard.core.persistence;

import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import byransha.ui.swing.desktop.src.dashboard.core.engine.DashboardLayout;

public class DashboardSaver {
	private Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private LayoutMapper mapper = new LayoutMapper();

	public void saveToFile(DashboardLayout layout, String path) {
		try {
			DashboardLayoutSave saveModel = mapper.exportLayout(layout);

			String json = gson.toJson(saveModel);

			FileWriter writer = new FileWriter(path);
			writer.write(json);
			writer.close();

			System.out.println("Layout sauvegardé dans : " + path);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}