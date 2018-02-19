package edu.asu.zoophy.rest.pipeline.glm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.asu.zoophy.rest.database.ZooPhyDAO;
import edu.asu.zoophy.rest.genbank.GenBankRecord;
import edu.asu.zoophy.rest.index.LuceneSearcher;
import edu.asu.zoophy.rest.pipeline.NewSequenceAligner;
import edu.asu.zoophy.rest.pipeline.utils.Normalizer;

/**
 * Generates templates for GLM Predictor files
 * @author devdemetri
 */
@Component("PredictorTemplateGenerator")
public class PredictorTemplateGenerator {

	@Autowired
	private ZooPhyDAO dao;
	
	@Autowired
	private LuceneSearcher indexSearcher;
	
	private final static String DELIMITER = "\t";
	
	/**
	 * Generates a GLM Predictor template String, ready to be written to a text file.
	 * @param accessions - list of target record accessions for template
	 * @return GLM Template String, ready to be written into a tab-delimited .txt file 
	 * @throws GLMException
	 */
	public String generateTemplate(List<String> accessions) throws GLMException {
		try {
			List<GenBankRecord> records;
			NewSequenceAligner aligner = new NewSequenceAligner(dao, indexSearcher);
			try {
				records = aligner.loadSequences(accessions, true, false);
			}
			catch (Exception e) {
				throw new GLMException("Error loading records for Predictor Template: "+e.getMessage(), "Error loading records for Predictor Template.");
			}
			Map<String, LocationPredictor> locationPredictors = new LinkedHashMap<String, LocationPredictor>();
			try {
				while (!records.isEmpty()) {
					GenBankRecord record = records.remove(0);
					String location = Normalizer.normalizeLocation(record.getGeonameLocation());
					LocationPredictor predictor = locationPredictors.get(location);
					if (predictor == null) {
						predictor = new LocationPredictor();
						predictor.setLatitude(record.getGeonameLocation().getLatitude());
						predictor.setLongitude(record.getGeonameLocation().getLongitude());
					}
					predictor.addSample();
					locationPredictors.put(location, predictor);
				}
			}
			catch (Exception e) {
				throw new GLMException("Error setting Predictors for Predictor Template: "+e.getMessage(), "Error setting Predictors for Predictor Template.");
			}
			StringBuilder template = new StringBuilder();
			template.append("state" + DELIMITER);
			template.append("lat" + DELIMITER);
			template.append("long" + DELIMITER);
			template.append("SampleSize" + DELIMITER);
			template.append("ExamplePredictor");
			template.append("\n");
			for (String location : locationPredictors.keySet()) {
				template.append(location + DELIMITER);
				LocationPredictor predictor = locationPredictors.get(location);
				template.append(predictor.getLatitude() + DELIMITER);
				template.append(predictor.getLongitude() + DELIMITER);
				template.append(predictor.getSampleSize() + DELIMITER);
				template.append("123.456");
				template.append("\n");
			}
			return template.toString();
		}
		catch (GLMException glme) {
			throw glme;
		}
		catch (Exception e) {
			throw new GLMException("Error generating Predictor Template: "+e.getMessage(), "Error generating Predictor Template.");
		}
	}
	
}
