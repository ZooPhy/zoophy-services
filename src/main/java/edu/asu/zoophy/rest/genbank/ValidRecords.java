package edu.asu.zoophy.rest.genbank;

import java.util.List;

/**
 * Valid records for job with distinct location count
 * @author kbhangal
 */
public class ValidRecords {
	private List<GenBankRecord> RecordList;
	private Integer distinctLocations;
	public List<GenBankRecord> getRecordList() {
		return RecordList;
	}
	public void setRecordList(List<GenBankRecord> recordList) {
		RecordList = recordList;
	}
	public Integer getDistinctLocations() {
		return distinctLocations;
	}
	public void setDistinctLocations(Integer distinctLocations) {
		this.distinctLocations = distinctLocations;
	}
	
}
