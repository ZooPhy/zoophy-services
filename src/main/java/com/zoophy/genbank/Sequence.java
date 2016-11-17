package com.zoophy.genbank;

/**
 * Viral Sequence object containing sequence details and the raw dna/rna sequence
 * @author devdemetri
 */
public class Sequence {

	private String accession;
	private String definition;
	private Integer taxId;
	private String organism;
	private String isolate;
	private String strain;
	private String collectionDate; 
	private String comment;
	private String rawSequence;
	private Integer segmentLength;
	private Publication pub;
	
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
	
	public Integer getTaxId() {
		return taxId;
	}
	
	public void setTaxId(Integer taxId) {
		this.taxId = taxId;
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
	
	public String getSequence() {
		return rawSequence;
	}
	
	public void setSequence(String rawSequence) {
		this.rawSequence = rawSequence;
	}
	
	public Integer getSegmentLength() {
		return segmentLength;
	}
	
	public void setSegmentLength(Integer segmentLength) {
		this.segmentLength = segmentLength;
	}
	
	public Publication getPub() {
		return pub;
	}
	
	public void setPub(Publication pub) {
		this.pub = pub;
	}

}
