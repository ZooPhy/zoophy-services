package edu.asu.zoophy.rest;

import java.util.LinkedList;
import java.util.List;

import edu.asu.zoophy.rest.genbank.InvalidRecords;

/**
 * ZooPhy Job Validation Details
 * @author devdemetri, kbhangal
 */
public class ValidationResults {
	
	private String error;
	private List<String> accessionsUsed;
	private List<InvalidRecords> accessionsRemoved;
	
	public ValidationResults() {
		error = null;
		accessionsUsed = new LinkedList<String>();
		accessionsRemoved = new LinkedList<InvalidRecords>();
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
	
	public List<InvalidRecords> getAccessionsRemoved() {
		return accessionsRemoved;
	}

	public void setAccessionsRemoved(List<InvalidRecords> accessionsRemoved) {
		this.accessionsRemoved = accessionsRemoved;
	}

}
