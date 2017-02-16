package edu.asu.zoophy.rest.pipeline.glm;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.asu.zoophy.rest.database.ZooPhyDAO;
import edu.asu.zoophy.rest.genbank.GenBankRecord;
import edu.asu.zoophy.rest.index.LuceneSearcher;
import edu.asu.zoophy.rest.pipeline.utils.GeonameDisjoiner;

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
			GeonameDisjoiner disjointer = new GeonameDisjoiner(indexSearcher);
			List<GenBankRecord> records = new LinkedList<GenBankRecord>();
			for (String accession : accessions) {
				records.add(dao.retrieveLightRecord(accession));
			}
			records = disjointer.disjoinRecords(records, false);
			Map<String, LocationPredictor> locationPredictors = new LinkedHashMap<String, LocationPredictor>();
			for (GenBankRecord record : records) {
				LocationPredictor predictor = locationPredictors.get(record.getGeonameLocation().getLocation());
				if (predictor == null) {
					predictor = new LocationPredictor();
					predictor.setLatitude(record.getGeonameLocation().getLatitude());
					predictor.setLongitude(record.getGeonameLocation().getLongitude());
				}
				predictor.addSample();
				locationPredictors.put(record.getGeonameLocation().getLocation(), predictor);
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
		catch (Exception e) {
			throw new GLMException("Error generating Predictor Template: "+e.getMessage(), "Error generating Predictor Template: "+e.getMessage());
		}
	}
	
}
