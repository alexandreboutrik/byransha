package byransha.ui.swing.desktop.src.dashboard.core.persistence;

import java.io.FileReader;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;

import byransha.ui.swing.desktop.src.dashboard.core.engine.DashboardLayout;
import byransha.ui.swing.desktop.src.dashboard.core.model.LayoutNode;

public class DashboardLoader {

	private Gson gson = new GsonBuilder()
			.registerTypeAdapter(NodeSave.class, (JsonDeserializer<NodeSave>) (json, typeOfT, context) -> {
				Map<String, Class<? extends NodeSave>> types = Map.of("panel", PanelSave.class, "split",
						SplitSave.class);
				JsonObject obj = json.getAsJsonObject();
				Class<? extends NodeSave> clazz = types.get(obj.get("type").getAsString());
				return context.deserialize(obj, clazz);
			}).create();

	private LayoutMapper mapper = new LayoutMapper();

	public DashboardLayout loadFromFile(String path) {
		try {
			FileReader reader = new FileReader(path);
			DashboardLayoutSave saveModel = gson.fromJson(reader, DashboardLayoutSave.class);

			LayoutNode rootNode = mapper.fromSaveNode(saveModel.root);

			DashboardLayout layout = new DashboardLayout();
			layout.setRoot(rootNode);

			System.out.println("Layout chargé depuis : " + path);
			return layout;

		} catch (Exception e) {
			System.out.println("Erreur lors du chargement de la save, création d'un layout vide");
			return new DashboardLayout();
		}
	}
}
