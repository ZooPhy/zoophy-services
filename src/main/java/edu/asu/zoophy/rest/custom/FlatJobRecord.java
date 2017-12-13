package edu.asu.zoophy.rest.custom;

/**
 * Main object for representing plain FASTA records for the file uploaded by users.
 * This is strictly a non-genbank implementation 
 * @author amagge
 */
public class FlatJobRecord {
	
	private String id;
	private String collectionDate; 
	private Long geonameID;
	private String rawSequence;

	public FlatJobRecord() {
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

	public Long getGeonameID() {
		return geonameID;
	}

	public void setGeonameID(Long geonameID) {
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
	
}