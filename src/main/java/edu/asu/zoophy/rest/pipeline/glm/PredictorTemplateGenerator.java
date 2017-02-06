package edu.asu.zoophy.rest.pipeline.glm;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.asu.zoophy.rest.database.ZooPhyDAO;

/**
 * Generates templates for GLM Predictor files
 * @author devdemetri
 */
@Component("PredictorTemplateGenerator")
public class PredictorTemplateGenerator {

	@Autowired
	private ZooPhyDAO dao;
	
	public String generateTemplate(List<String> accessions) {
		StringBuilder template = new StringBuilder();
		//TODO: generate template
		return template.toString();
	}
	
}
