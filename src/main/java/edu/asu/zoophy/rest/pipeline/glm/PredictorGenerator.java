package edu.asu.zoophy.rest.pipeline.glm;

import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.asu.zoophy.rest.database.DaoException;
import edu.asu.zoophy.rest.database.ZooPhyDAO;

/**
 * Constructs batch GLM predictor files for BEAST GLM
 * @author devdemetri
 */
public class PredictorGenerator {
	
	private static Logger log = Logger.getLogger("PredictorGenerator");
	final private int START_YEAR;
	final private int END_YEAR;
	final private String TXT_FILE_PATH;
	final private ZooPhyDAO dao;
	private Map<String, StatePredictor> statePredictors;
	
	public PredictorGenerator(String filePath, int startYear, int endYear, Set<String> stateList, ZooPhyDAO dao) {
		START_YEAR = startYear;
		END_YEAR = endYear;
		TXT_FILE_PATH = filePath;
		statePredictors = new LinkedHashMap<String, StatePredictor>(stateList.size());
		this.dao = dao;
		for (String state : stateList) {
			statePredictors.put(state, null);
		}
	}
	
	/**
	 * Generates a tab delimited text file ready to be used in BEAST GLM
	 * @return Path to Predictors text file in BEAST GLM format
	 * @throws GLMException
	 */
	public String generatePredictorsFile(Map<String, Integer> occurences) throws GLMException {
		log.info("Generating Predictor File...");
		try {
			loadPredictors(occurences);
			writePredictors();
			log.info("Generated Predictor File.");
			return TXT_FILE_PATH;
		}
		catch (GLMException glme) {
			throw glme;
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "ERROR generating predictors file: "+e.getMessage());
			throw new GLMException("ERROR generating predictors file: "+e.getMessage(), null);
		}
	}

	/**
	 * Loads all applicable predictors
	 * @param occurences 
	 * @throws DaoException
	 */
	private void loadPredictors(Map<String, Integer> occurences) throws GLMException {
		log.info("Loading Predictors...");
		try {
			List<Predictor> rawPredictors;
			StatePredictor averagedPredictors;
			for (String state : statePredictors.keySet()) {
				String normalizedState;
				if (state.contains("-")) {
					int split = state.indexOf("-");
					normalizedState = (Character.toUpperCase(state.charAt(0)) + state.substring(1, split) + " " + Character.toUpperCase(state.charAt(split+1)) + state.substring(split+2)).trim();
				}
				else {
					normalizedState = (Character.toUpperCase(state.charAt(0)) + state.substring(1)).trim();
				}
				rawPredictors = dao.retrievePredictors(normalizedState);
				Predictor tempPredictor;
				double totalPopulation = 0;
				double populationYears = 0;
				double totalMedianAge = 0;
				double medianAgeDecades = 0;
				averagedPredictors = new StatePredictor();
				for (int i = 0; i < rawPredictors.size(); i++) {
					tempPredictor = rawPredictors.get(i);
					switch (tempPredictor.getName()) {
						case "Latitude":
							averagedPredictors.setLatitude(tempPredictor.getValue());
							break;
						case "Longitude":
							averagedPredictors.setLongitude(tempPredictor.getValue());
							break;
						case "AverageElevation":
							averagedPredictors.setElevation(tempPredictor.getValue());
							break;
						case "AnnualAverageTemperatue":
							averagedPredictors.setTemperature(tempPredictor.getValue());
							break;
						case "Population":
							if (tempPredictor.getYear() >= START_YEAR && tempPredictor.getYear() <= END_YEAR) {
								totalPopulation += tempPredictor.getValue();
								populationYears++;
							}
							break;
						case "MedianAge":
							if (tempPredictor.getYear() >= START_YEAR-5 && tempPredictor.getYear() <= END_YEAR+5) {
								totalMedianAge += tempPredictor.getValue();
								medianAgeDecades++;
							}
							break;
					}
				}
				rawPredictors.clear();
				tempPredictor = null;
				averagedPredictors.setAveragePopulation(totalPopulation/populationYears);
				averagedPredictors.setAverageMedianAge(totalMedianAge/medianAgeDecades);
				averagedPredictors.setSampleSize(occurences.get(state));
				statePredictors.put(state, averagedPredictors);
			}
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Failed to load Predictors: "+e.getMessage());
			throw new GLMException("Failed to load Predictors: "+e.getMessage(), null);
		}
	}
	
	/**
	 * Writes the predictors text file to TXT_FILE_PATH
	 * @throws GLMException 
	 */
	private void writePredictors() throws GLMException {
		log.info("Writing Predictors...");
		PrintWriter predictorWriter = null;
		try {
			final String DELIMITER = "\t";
			StringBuilder txtBuilder = new StringBuilder();
			//TODO: for now we are just using Distance (needs Lat and Long), Temperature, Precipitation, and SampleSize as predictors
			txtBuilder.append("state" + DELIMITER);
			txtBuilder.append("lat" + DELIMITER);
			txtBuilder.append("long" + DELIMITER);
//			txtBuilder.append("elevation" + DELIMITER);
			txtBuilder.append("temperature" + DELIMITER);
//			txtBuilder.append("avg_population" + DELIMITER);
//			txtBuilder.append("median_age");
			txtBuilder.append("SampleSize");
			txtBuilder.append("\n");
			for (String state : statePredictors.keySet()) {
				txtBuilder.append(state + DELIMITER);
				StatePredictor predictors = statePredictors.get(state);
				txtBuilder.append(predictors.getLatitude() + DELIMITER);
				txtBuilder.append(predictors.getLongitude() + DELIMITER);
//				txtBuilder.append(predictors.getElevation() + DELIMITER);
				txtBuilder.append(predictors.getTemperature() + DELIMITER);
//				txtBuilder.append(predictors.getAveragePopulation() + DELIMITER);
//				txtBuilder.append(predictors.getAverageMedianAge());
				txtBuilder.append(predictors.getSampleSize());
				txtBuilder.append("\n");
			}
			predictorWriter = new PrintWriter(TXT_FILE_PATH);
			predictorWriter.write(txtBuilder.toString());
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Failed to write Predictors: "+e.getMessage());
			throw new GLMException("Failed to write Predictors: "+e.getMessage(), null);
		}
		finally {
			if (predictorWriter != null) {
				predictorWriter.close();
			}
		}
	}
	
	/**
	 * Creates a GLM Predictors file from user given predictors
	 * @param path
	 * @param predictors
	 * @throws GLMException
	 */
	public static void writeCustomPredictorsFile(String path, Map<String, List<Predictor>> predictors) throws GLMException {
		try {
			log.info("Writing custom predictors...");
			//TODO write custom predictors
			log.info("Finished writing custom predictors.");
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Invalid Custom Predictors: "+e.getMessage());
			throw new GLMException("Invalid Custom Predictors: "+e.getMessage(), "Invalid Custom Predictors");
		}
	}
	
}
