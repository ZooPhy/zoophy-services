package edu.asu.zoophy.rest;

import java.util.List;
import java.util.Map;

import edu.asu.zoophy.rest.pipeline.glm.Predictor;

/**
 * Object to encapsulate parameters for ZooPhy jobs
 * @author devdemetri
 */
public class JobParameters {
	
	private String replyEmail;
	private String jobName;
	private List<String> accessions;
	private boolean useGLM = false;
	private Map<String, List<Predictor>> predictors;
	
	public JobParameters() {
		
	}

	public String getReplyEmail() {
		return replyEmail;
	}

	public void setReplyEmail(String replyEmail) {
		this.replyEmail = replyEmail;
	}

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public List<String> getAccessions() {
		return accessions;
	}

	public void setAccessions(List<String> accessions) {
		this.accessions = accessions;
	}

	public boolean isUsingGLM() {
		return useGLM;
	}

	public void setUseGLM(boolean useGLM) {
		this.useGLM = useGLM;
	}

	public Map<String, List<Predictor>> getPredictors() {
		return predictors;
	}

	public void setPredictors(Map<String, List<Predictor>> predictors) {
		this.predictors = predictors;
	}
	
}
