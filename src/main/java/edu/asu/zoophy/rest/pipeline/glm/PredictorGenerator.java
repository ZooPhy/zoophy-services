package edu.asu.zoophy.rest.pipeline.glm;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.asu.zoophy.rest.database.ZooPhyDAO;

/**
 * Constructs batch GLM predictor files
 * @author devdemetri
 */
public class PredictorGenerator {
	
	private static Logger log = Logger.getLogger("PredictorGenerator");
	
	public static File generatePredictorsFile(String filePath, int startYear, int endYear, ZooPhyDAO dao) throws GLMException {
		log.info("Generating Predictor File...");
		try {
			File predictorsFile = new File(filePath);
			//TODO: finish generating file
			log.info("Generated Predictor File.");
			return predictorsFile;
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "ERROR generating predictors file: "+e.getMessage());
			throw new GLMException("ERROR generating predictors file: "+e.getMessage(), null);
		}
	}

}
