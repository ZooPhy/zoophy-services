package edu.asu.zoophy.rest.custom;

import edu.asu.zoophy.rest.genbank.Location;

/**
 * Main object for representing plain FASTA records for the file uploaded by users.
 * This is strictly a non-genbank implementation 
 * @author amagge
 */
public class FastaRecord {
	
	private String accession;
	private String collectionDate; 
	private String rawSequence;
	private Location geonameLocation;

	public FastaRecord() {
	}
	
	public FastaRecord(String accession, String collectionDate, String rawSequence, Location geonameLocation) {
		this.accession = accession;
		this.collectionDate = collectionDate;
		this.rawSequence = rawSequence;
		this.geonameLocation = geonameLocation;
	}

	public String getAccession() {
		return accession;
	}

	public void setAccession(String accession) {
		this.accession = accession;
	}

	public String getCollectionDate() {
		return collectionDate;
	}

	public void setCollectionDate(String collectionDate) {
		this.collectionDate = collectionDate;
	}

	public String getRawSequence() {
		return rawSequence;
	}

	public void setRawSequence(String rawSequence) {
		this.rawSequence = rawSequence;
	}

	public Location getGeonameLocation() {
		return geonameLocation;
	}

	public void setGeonameLocation(Location geonameLocation) {
		this.geonameLocation = geonameLocation;
	}
	
}