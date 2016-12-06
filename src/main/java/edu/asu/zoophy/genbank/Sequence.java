package edu.asu.zoophy.genbank;

/**
 * Viral Sequence object containing sequence details and the raw dna/rna sequence
 * @author devdemetri
 */
public class Sequence {

	private String accession;
	private String definition;
	private Integer taxID;
	private String organism;
	private String isolate;
	private String strain;
	private String collectionDate; 
	private String comment;
	private String rawSequence;
	private Integer segmentLength;
	
	public Sequence() {
		
	}
	
	public String getAccession() {
		return accession;
	}
	
	public void setAccession(String accession) {
		this.accession = accession;
	}
	
	public String getDefinition() {
		return definition;
	}
	
	public void setDefinition(String definition) {
		this.definition = definition;
	}
	
	public Integer getTaxID() {
		return taxID;
	}
	
	public void setTaxID(Integer taxID) {
		this.taxID = taxID;
	}
	
	public String getOrganism() {
		return organism;
	}
	
	public void setOrganism(String organism) {
		this.organism = organism;
	}
	
	public String getIsolate() {
		return isolate;
	}
	
	public void setIsolate(String isolate) {
		this.isolate = isolate;
	}
	
	public String getStrain() {
		return strain;
	}
	public void setStrain(String strain) {
		this.strain = strain;
	}
	
	public String getCollectionDate() {
		return collectionDate;
	}
	
	public void setCollectionDate(String collectionDate) {
		this.collectionDate = collectionDate;
	}
	
	public String getComment() {
		return comment;
	}
	
	public void setComment(String comment) {
		this.comment = comment;
	}
	
	public String getRawSequence() {
		return rawSequence;
	}
	
	public void setRawSequence(String rawSequence) {
		this.rawSequence = rawSequence;
	}
	
	public Integer getSegmentLength() {
		return segmentLength;
	}
	
	public void setSegmentLength(Integer segmentLength) {
		this.segmentLength = segmentLength;
	}

}
