package edu.asu.zoophy.rest;


/**
 * Main object for representing job records.
 * Thus implementation is for both fasta and genBank search 
 * @author devdemetri, kbhangal
 */
public class JobRecord {
	
	private String id;
	private String collectionDate; 
	private String geonameID;
	private String rawSequence;
	private int resourceSource;

	public JobRecord() {
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCollectionDate() {
		return collectionDate;
	}

	public void setCollectionDate(String collectionDate) {
		this.collectionDate = collectionDate;
	}

	public String getGeonameID() {
		return geonameID;
	}

	public void setGeonameID(String geonameID) {
		this.geonameID = geonameID;
	}

	public String getRawSequence() {
		return rawSequence;
	}

	public void setRawSequence(String rawSequence) {
		this.rawSequence = rawSequence;
	}
	
	public String getStringRep() {
		return id ;
	}
	


	public int getResourceSource() {
		return resourceSource;
	}

	public void setResourceSource(int resourceSource) {
		this.resourceSource = resourceSource;
	}

}
