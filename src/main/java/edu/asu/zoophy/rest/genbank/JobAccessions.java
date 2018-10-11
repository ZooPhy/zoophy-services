package edu.asu.zoophy.rest.genbank;

import java.util.List;
import java.util.Set;

/**
 * Job Accessions with distinct location count
 * @author kbhangal
 */
public class JobAccessions {
	private Set<String> validAccessions;
	private List<InvalidRecords> invalidRecordList;
 	private Integer distinctLocations;
 	
	public Set<String> getValidAccessions() {
		return validAccessions;
	}
	public void setValidAccessions(Set<String> validAccessions) {
		this.validAccessions = validAccessions;
	}
	public Integer getDistinctLocations() {
		return distinctLocations;
	}
	public void setDistinctLocations(Integer distinctLocations) {
		this.distinctLocations = distinctLocations;
	}
	public List<InvalidRecords> getInvalidRecordList() {
		return invalidRecordList;
	}
	public void setInvalidRecordList(List<InvalidRecords> invalidRecordList) {
		this.invalidRecordList = invalidRecordList;
	}
 	
}
