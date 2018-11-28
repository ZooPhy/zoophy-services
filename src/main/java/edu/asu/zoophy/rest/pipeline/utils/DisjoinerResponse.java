package edu.asu.zoophy.rest.pipeline.utils;

import java.util.List;
import java.util.Set;

import edu.asu.zoophy.rest.genbank.GenBankRecord;
import edu.asu.zoophy.rest.genbank.InvalidRecords;
import edu.asu.zoophy.rest.genbank.Location;
/**
 * Records for job with distinct locations.
 * It is used as a response while disjoining each country
 * @author kbhangal
 */
public class DisjoinerResponse {
	private List<GenBankRecord> validRecordList;		
	private List<InvalidRecords> invalidRecordList;
	private Set<Location> distinctLocations;
	
	public DisjoinerResponse(List<GenBankRecord> validRecordList, List<InvalidRecords> invalidRecordList, Set<Location> distinctLocations) {
		this.validRecordList = validRecordList;
		this.distinctLocations = distinctLocations;
		this.invalidRecordList = invalidRecordList;
	}
	public List<GenBankRecord> getValidRecordList() {
		return validRecordList;
	}
	public void setValidRecordList(List<GenBankRecord> validRecordList) {
		this.validRecordList = validRecordList;
	}
	public Set<Location> getDistinctLocations() {
		return distinctLocations;
	}
	public void setDistinctLocations(Set<Location> distinctLocations) {
		this.distinctLocations = distinctLocations;
	}
	public List<InvalidRecords> getInvalidRecordList() {
		return invalidRecordList;
	}
	public void setInvalidRecordList(List<InvalidRecords> invalidRecordList) {
		this.invalidRecordList = invalidRecordList;
	}
	
}
