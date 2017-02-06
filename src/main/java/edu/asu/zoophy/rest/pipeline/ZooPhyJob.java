package edu.asu.zoophy.rest.pipeline;

import java.util.List;
import java.util.Map;

import edu.asu.zoophy.rest.pipeline.glm.Predictor;

/**
 * Encapsulates related info for a ZooPhy Job
 * @author devdemetri
 */
public final class ZooPhyJob {
	
	private final String ID;
	private final String JOB_NAME;
	private final String REPLY_EMAIL;
	private final boolean USE_GLM;
	private final boolean USE_CUSTOM_PREDICTORS;
	private final Map<String, List<Predictor>> predictors;
	
	public ZooPhyJob(String id, String name, String email, boolean useGLM, Map<String, List<Predictor>> predictors) {
		ID = id;
		JOB_NAME = name;
		REPLY_EMAIL = email;
		USE_GLM = useGLM;
		if (predictors == null || predictors.isEmpty()) {
			USE_CUSTOM_PREDICTORS = false;
			this.predictors = null;
		}
		else {
			this.predictors = predictors;
			USE_CUSTOM_PREDICTORS = true;
		}
	}
	
	public String getID() {
		return ID;
	}
	
	public String getJobName() {
		return JOB_NAME;
	}
	
	public String getReplyEmail() {
		return REPLY_EMAIL;
	}
	
	public boolean isUsingGLM() {
		return USE_GLM;
	}

	public boolean isUsingCustomPredictors() {
		return USE_CUSTOM_PREDICTORS;
	}

	public Map<String, List<Predictor>> getPredictors() {
		return predictors;
	}
	
}
