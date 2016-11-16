package com.zoophy.genbank;

/**
 * 
 * @author devdemetri
 *
 */
public class Publication {
	
	private Integer pubId;
	private Integer pubmedId;
	private String centralId;
	private String authors;
	private String title;
	private String journal;
	
	public Integer getPubId() {
		return pubId;
	}
	
	public void setPubId(Integer pubId) {
		this.pubId = pubId;
	}
	
	public Integer getPubmedId() {
		return pubmedId;
	}
	
	public void setPubmedId(Integer pubmedId) {
		this.pubmedId = pubmedId;
	}
	
	public String getCentralId() {
		return centralId;
	}
	
	public void setCentralId(String centralId) {
		this.centralId = centralId;
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
