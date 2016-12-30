package edu.asu.zoophy.genbank;

/**
 * Virus gene
 * @author devdemetri
 */
public class Gene {
	
	private String accession; 
	private String name;
	
	public Gene() {
		
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
