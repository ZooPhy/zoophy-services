package edu.asu.zoophy.rest.pipeline.glm;

import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.asu.zoophy.rest.database.DaoException;
import edu.asu.zoophy.rest.database.ZooPhyDAO;
import edu.asu.zoophy.rest.genbank.Location;
import edu.asu.zoophy.rest.index.LuceneHierarchySearcher;
import edu.asu.zoophy.rest.index.LuceneSearcherException;
import edu.asu.zoophy.rest.pipeline.utils.Normalizer;

/**
 * Constructs batch GLM predictor files for BEAST GLM
 * @author devdemetri, kbhangal
 */
public class PredictorGenerator {
	
	private final static Logger log = Logger.getLogger("PredictorGenerator");
	private final LuceneHierarchySearcher hierarchyIndexSearcher;
	final private int START_YEAR;
	final private int END_YEAR;
	final private String TXT_FILE_PATH;
	final private ZooPhyDAO dao;
	final private static String DELIMITER = "\t";
	private Map<String, StatePredictor> statePredictors;
	
	public PredictorGenerator(String filePath, int startYear, int endYear, Set<String> stateList, ZooPhyDAO dao, LuceneHierarchySearcher hierarchyIndexSearcher) {
		this.hierarchyIndexSearcher = hierarchyIndexSearcher;
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
			throw new GLMException("ERROR generating predictors file: "+e.getMessage(), "Error generating predictors file.");
		}
	}
	
	/**
	 * Generates a tab delimited text file ready to be used in default BEAST GLM
	 * @return Path to Predictors text file in BEAST GLM format
	 * @throws GLMException
	 */
	public void generateDefaultPredictorsFile(Map<String, Integer> occurences, Set<Location> uniqueLocations) throws GLMException{
		double defaultPopulation = 0.0000001;
		Set<String> geoNameIDs = new LinkedHashSet<>();
		for(Location location: uniqueLocations) {
			geoNameIDs.add(location.getGeonameID().toString());
		}
		try {
			Map<String, Location> predictorData = hierarchyIndexSearcher.findGeonameLocations(geoNameIDs);
			
			log.info("Writing Predictors...");
			PrintWriter predictorWriter = null;
			try {
				StringBuilder txtBuilder = new StringBuilder();
				txtBuilder.append("location" + DELIMITER);
				txtBuilder.append("lat" + DELIMITER);
				txtBuilder.append("long" + DELIMITER);
				txtBuilder.append("population" + DELIMITER);
				txtBuilder.append("SampleSize");
				txtBuilder.append("\n");
				for (Location location: uniqueLocations) {
					txtBuilder.append(Normalizer.normalizeLocation(location) + DELIMITER);
					txtBuilder.append(location.getLatitude() + DELIMITER);
					txtBuilder.append(location.getLongitude() + DELIMITER);
					if(predictorData.get(location.getGeonameID().toString()).getPopulation() == 0) {
						txtBuilder.append(defaultPopulation + DELIMITER);
					}else {
						txtBuilder.append(predictorData.get(location.getGeonameID().toString()).getPopulation().doubleValue() + DELIMITER);
					}
					txtBuilder.append(occurences.get(Normalizer.normalizeLocation(location)));
					txtBuilder.append("\n");
				}
				predictorWriter = new PrintWriter(TXT_FILE_PATH);
				predictorWriter.write(txtBuilder.toString());
			}
			catch (Exception e) {
				log.log(Level.SEVERE, "Failed to write Predictors: "+e.getMessage());
				throw new GLMException("Failed to write Predictors: "+e.getMessage(), "Error writing Predictors file.");
			}
			finally {
				if (predictorWriter != null) {
					predictorWriter.close();
				}
			}
		} catch (LuceneSearcherException e) {
			log.log(Level.SEVERE, "ERROR generating predictors file: "+e.getMessage());
			throw new GLMException("ERROR generating predictors file: "+e.getMessage(), "Error generating predictors file.");
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
				if (state.equalsIgnoreCase("district-of-columbia")) {
					normalizedState = "District of Columbia";
				}
				else if (state.contains("-")) {
					int split = state.indexOf("-");
					normalizedState = (Character.toUpperCase(state.charAt(0)) + state.substring(1, split) + " " + Character.toUpperCase(state.charAt(split+1)) + state.substring(split+2)).trim();
				}
				else {
					normalizedState = (Character.toUpperCase(state.charAt(0)) + state.substring(1)).trim();
				}
				rawPredictors = dao.retrieveDefaultPredictors(normalizedState);
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
			throw new GLMException("Failed to load Predictors: "+e.getMessage(), "Error loading Predictors");
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
			StringBuilder txtBuilder = new StringBuilder();
			txtBuilder.append("state" + DELIMITER);
			txtBuilder.append("lat" + DELIMITER);
			txtBuilder.append("long" + DELIMITER);
//			txtBuilder.append("elevation" + DELIMITER); not needed at this time
			txtBuilder.append("temperature" + DELIMITER);
			txtBuilder.append("population" + DELIMITER);
//			txtBuilder.append("median_age"); not needed at this time
			txtBuilder.append("SampleSize");
			txtBuilder.append("\n");
			for (String state : statePredictors.keySet()) {
				txtBuilder.append(state + DELIMITER);
				StatePredictor predictors = statePredictors.get(state);
				if (hasNull(predictors)) {
					log.log(Level.SEVERE, "Found null Predictor for : "+state);
					throw new GLMException("Found null Predictor for : "+state, "Found null Predictor for : "+state);
				}
				txtBuilder.append(predictors.getLatitude() + DELIMITER);
				txtBuilder.append(predictors.getLongitude() + DELIMITER);
//				txtBuilder.append(predictors.getElevation() + DELIMITER); not needed at this time
				txtBuilder.append(predictors.getTemperature() + DELIMITER);
				txtBuilder.append(predictors.getAveragePopulation() + DELIMITER);
//				txtBuilder.append(predictors.getAverageMedianAge()); not needed at this time
				txtBuilder.append(predictors.getSampleSize());
				txtBuilder.append("\n");
			}
			predictorWriter = new PrintWriter(TXT_FILE_PATH);
			predictorWriter.write(txtBuilder.toString());
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Failed to write Predictors: "+e.getMessage());
			throw new GLMException("Failed to write Predictors: "+e.getMessage(), "Error writing Predictors file.");
		}
		finally {
			if (predictorWriter != null) {
				predictorWriter.close();
			}
		}
	}
	
	/**
	 * Checks for null values in required fields
	 * @param predictors Predictor values to check
	 * @return true if any required Predictor values are null, false otherwise
	 */
	private boolean hasNull(StatePredictor predictors) {
		if (predictors.getLatitude() == null || predictors.getLongitude() == null || predictors.getTemperature() == null || predictors.getTemperature() == 0 || predictors.getAveragePopulation() == null || predictors.getAveragePopulation() == 0 || predictors.getSampleSize() == 0) {
			return true;
		}
		return false;
	}

	/**
	 * Creates a GLM Predictors file from user given predictors
	 * @param path
	 * @param predictors
	 * @throws GLMException
	 */
	public static void writeCustomPredictorsFile(String path, Map<String, List<Predictor>> predictors) throws GLMException {
		PrintWriter predictorWriter = null;
		try {
			log.info("Writing custom predictors...");
			Set<String> states = predictors.keySet();
			boolean missingHeader = true;
			StringBuilder txtBuilder = new StringBuilder();
			for (String state : states) {
				List<Predictor> linePredictors = predictors.get(state);
				if (missingHeader) {
					txtBuilder.append("state"+DELIMITER);
					for (int i = 0; i < linePredictors.size(); i++) {
						txtBuilder.append(linePredictors.get(i).getName().trim());
						if (i < linePredictors.size()-1) {
							txtBuilder.append(DELIMITER);
						}
					}
					txtBuilder.append("\n");
					missingHeader = false;
				}
				txtBuilder.append(state+DELIMITER);
				for (int i = 0; i < linePredictors.size(); i++) {
					txtBuilder.append(linePredictors.get(i).getValue());
					if (i < linePredictors.size()-1) {
						txtBuilder.append(DELIMITER);
					}
				}
				txtBuilder.append("\n");
			}
			predictorWriter = new PrintWriter(path);
			predictorWriter.write(txtBuilder.toString());
			log.info("Finished writing custom predictors.");
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Invalid Custom Predictors: "+e.getMessage());
			throw new GLMException("Invalid Custom Predictors: "+e.getMessage(), "Invalid Custom Predictors");
		}
		finally {
			if (predictorWriter != null) {
				predictorWriter.close();
			}
		}
	}
	
}
