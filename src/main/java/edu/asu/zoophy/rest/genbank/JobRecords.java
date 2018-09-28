package edu.asu.zoophy.rest.genbank;

import java.util.List;

/**
 * Records for job with distinct location count
 * @author kbhangal
 */
public class JobRecords {
	private List<GenBankRecord> validRecordList;		
	private List<InvalidRecords> invalidRecordList;
	private Integer distinctLocations;
	
	public List<GenBankRecord> getValidRecordList() {
		return validRecordList;
	}
	public void setValidRecordList(List<GenBankRecord> validRecordList) {
		this.validRecordList = validRecordList;
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
