package edu.asu.zoophy.rest.genbank;

import java.util.List;
/**
 * Invalid Accessions for job with reason of exclusion
 * @author kbhangal
 */
public class InvalidRecords {
	private List<ExcludedRecords> excludedRecords;
	private String reason;
	
	public InvalidRecords(List<ExcludedRecords> excludedRecords, String reason) {
		this.excludedRecords = excludedRecords;
		this.reason = reason;
	}
	
	public List<ExcludedRecords> getExcludedRecords() {
		return excludedRecords;
	}
	public void setAccessions(List<ExcludedRecords> excludedRecords) {
		this.excludedRecords = excludedRecords;
	}
	public String getReason() {
		return reason;
	}
	public void setReason(String reason) {
		this.reason = reason;
	}

}
