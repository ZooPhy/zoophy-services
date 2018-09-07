package edu.asu.zoophy.rest.genbank;

import java.util.Set;

public class ValidAccessions {
	private Set<String> accessions;
 	private String distinctLocations;
	public Set<String> getAccessions() {
		return accessions;
	}
	public void setAccessions(Set<String> accessions) {
		this.accessions = accessions;
	}
	public String getDistinctLocations() {
		return distinctLocations;
	}
	public void setDistinctLocations(String distinctLocations) {
		this.distinctLocations = distinctLocations;
	}
 	
}
