package edu.asu.zoophy.rest.custom;

import java.util.List;

/**
 * Object for records and columns that need to be downloaded 
 * @author kbhangal
 */
public class DownloadRecords {
	
	private List<String> accessions;
	private List<String> columns;
	
	public List<String> getAccessions() {
		return accessions;
	}
	public void setAccessions(List<String> accessions) {
		this.accessions = accessions;
	}
	public List<String> getColumns() {
		return columns;
	}
	public void setColumns(List<String> columns) {
		this.columns = columns;
	}

}
