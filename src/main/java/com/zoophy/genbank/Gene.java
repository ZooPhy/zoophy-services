package com.zoophy.genbank;

/**
 * Virus gene
 * @author devdemetri
 */
public class Gene {
	
	private Long id;
	private String accession; 
	private String name;
	
	public Gene() {
		
	}
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
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
	
}
