package edu.asu.zoophy.rest;

import java.util.List;
import java.util.Map;

import edu.asu.zoophy.rest.custom.FastaRecord;

/**
 * Object to represent response for uploaded FASTA file
 * @author kbhangal
 */
public class FastaUploadResponse {
	private List<FastaRecord> fastaRecord;
	private Map<String, List<String>> invalidRecords;
	
	FastaUploadResponse(List<FastaRecord> fastaRecord, Map<String, List<String>> invalidRecords){
		this.fastaRecord = fastaRecord;
		this.invalidRecords = invalidRecords;
	}

	public List<FastaRecord> getFastaRecord() {
		return fastaRecord;
	}

	public void setFastaRecord(List<FastaRecord> fastaRecord) {
		this.fastaRecord = fastaRecord;
	}

	public Map<String, List<String>> getInvalidRecords() {
		return invalidRecords;
	}

	public void setInvalidRecords(Map<String, List<String>> invalidRecords) {
		this.invalidRecords = invalidRecords;
	}
	

}
