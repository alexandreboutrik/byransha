package byransha.nodes.lab;

import java.io.File;
import java.io.IOException;

import byransha.graph.BNode;

/*
 * https://codimd.math.cnrs.fr/_ivy9aRUQK2o4ue-p9RHKg?both
 * https://cran.r-project.org/web/classifications/ACM.html
 */

public class I3S extends Lab {

	public I3S(BNode g) {
		super(g);
		name.set("I3S");

		var lakeD = new File(g().byransha.homeDirectory, "data_lake");

		if (lakeD.exists()) {
			var lake = new DataLake(g(), lakeD);
		} else {
			System.out.println("data lake not found at " + lakeD.getAbsolutePath());
		}
	}
}
