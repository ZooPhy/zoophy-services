package com.zoophy.genbank;

/**
 * Pubmed publication
 * 
 * @author devdemetri
 */
public class Publication {

	private Integer pubMedID;
	private String centralID;
	private String authors;
	private String title;
	private String journal;

	public Publication() {

	}

	public Integer getPubMedID() {
		return pubMedID;
	}

	public void setPubMedID(Integer pubmedID) {
		this.pubMedID = pubmedID;
	}

	public String getCentralID() {
		return centralID;
	}

	public void setCentralID(String centralID) {
		this.centralID = centralID;
	}

	public String getAuthors() {
		return authors;
	}

	public void setAuthors(String authors) {
		this.authors = authors;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getJournal() {
		return journal;
	}

	public void setJournal(String journal) {
		this.journal = journal;
	}

}
