package edu.asu.zoophy.rest.genbank;

import java.util.List;
/**
 * Invalid Accessions for job with reason of exclusion
 * @author kbhangal
 */
public class InvalidRecords {
	private List<String> accessions;
	private String reason;
	
	public List<String> getAccessions() {
		return accessions;
	}
	public void setAccessions(List<String> accessions) {
		this.accessions = accessions;
	}
	public String getReason() {
		return reason;
	}
	public void setReason(String reason) {
		this.reason = reason;
	}

}
