package edu.asu.zoophy.rest.genbank;

import java.util.List;

public class ValidRecords {
	private List<GenBankRecord> RecordList;
	private String distinctLocations;
	public List<GenBankRecord> getRecordList() {
		return RecordList;
	}
	public void setRecordList(List<GenBankRecord> recordList) {
		RecordList = recordList;
	}
	public String getDistinctLocations() {
		return distinctLocations;
	}
	public void setDistinctLocations(String distinctLocations) {
		this.distinctLocations = distinctLocations;
	}
	
}
