package edu.asu.zoophy.rest.genbank;

/**
 * Records excluded from the job 
 * @author kbhangal
 */
public class ExcludedRecords {
	private String accession;
	private String adminLevel;
	
	public ExcludedRecords(String accession, String adminLevel){
		this.accession = accession;
		this.adminLevel = adminLevel;
	}
	
	public String getAccession() {
		return accession;
	}
	public void setAccession(String accession) {
		this.accession = accession;
	}
	public String getAdminLevel() {
		return adminLevel;
	}
	public void setAdminLevel(String adminLevel) {
		this.adminLevel = adminLevel;
	}
	
}
