package edu.asu.zoophy.rest.pipeline.utils;

import java.util.Map;
import java.util.Set;

import edu.asu.zoophy.rest.genbank.JobRecords;

/**
 * Records for job with map of locations and their ancestors
 * @author kbhangal
 */
public class DisjoinerCleanUpResponse {
	private JobRecords jobRecords;
	private Map<String,Set<Long>> ancestors;
	
	public DisjoinerCleanUpResponse(JobRecords jobRecords, Map<String,Set<Long>> ancestors) {
		this.jobRecords = jobRecords;
		this.ancestors = ancestors;
	}
	public JobRecords getJobRecords() {
		return jobRecords;
	}
	public Map<String, Set<Long>> getAncestors() {
		return ancestors;
	}
}
