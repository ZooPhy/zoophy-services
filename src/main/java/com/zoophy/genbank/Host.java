package com.zoophy.genbank;

/**
 * Virus host
 * @author devdemetri
 */
public class Host {
	
	private String accession;
	private String name;
	private Integer taxon;
	
	public Host() {
		
	}
	
	public String getAccession() {
		return accession;
	}
	
	public void setAccession(String accession) {
		this.accession = accession;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public Integer getTaxon() {
		return taxon;
	}
	
	public void setTaxon(Integer taxon) {
		this.taxon = taxon;
	}
	
}
