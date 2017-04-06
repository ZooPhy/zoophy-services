package edu.asu.zoophy.rest;

import java.util.LinkedList;
import java.util.List;

/**
 * ZooPhy Job Validation Details
 * @author devdemetri
 */
public class ValidationResults {
	
	private String error;
	private List<String> accessionsUsed;
	private List<String> accessionsRemoved;
	
	public ValidationResults() {
		error = null;
		accessionsUsed = new LinkedList<String>();
		accessionsRemoved = new LinkedList<String>();
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public List<String> getAccessionsUsed() {
		return accessionsUsed;
	}

	public void setAccessionsUsed(List<String> accessionsUsed) {
		this.accessionsUsed = accessionsUsed;
	}
	
	public List<String> getAccessionsRemoved() {
		return accessionsRemoved;
	}

	public void setAccessionsRemoved(List<String> accessionsRemoved) {
		this.accessionsRemoved = accessionsRemoved;
	}

}
