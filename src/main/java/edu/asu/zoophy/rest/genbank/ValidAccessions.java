package edu.asu.zoophy.rest.genbank;

import java.util.Set;

/**
 * Valid Accessions for job with distinct location count
 * @author kbhangal
 */
public class ValidAccessions {
	private Set<String> accessions;
 	private Integer distinctLocations;
	public Set<String> getAccessions() {
		return accessions;
	}
	public void setAccessions(Set<String> accessions) {
		this.accessions = accessions;
	}
	public Integer getDistinctLocations() {
		return distinctLocations;
	}
	public void setDistinctLocations(Integer distinctLocations) {
		this.distinctLocations = distinctLocations;
	}
 	
}
