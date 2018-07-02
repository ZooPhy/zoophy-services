package edu.asu.zoophy.rest.custom;

import java.util.List;

import edu.asu.zoophy.rest.JobRecord;

/**
 * Object for records and columns that need to be downloaded 
 * @author kbhangal
 */
public class DownloadRecords {
	
	private List<JobRecord> accessions;
	private List<String> columns;
	
	public List<JobRecord> getAccessions() {
		return accessions;
	}
	public void setAccessions(List<JobRecord> accessions) {
		this.accessions = accessions;
	}
	public List<String> getColumns() {
		return columns;
	}
	public void setColumns(List<String> columns) {
		this.columns = columns;
	}

}
