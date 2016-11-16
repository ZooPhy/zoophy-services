package com.zoophy.genbank;

/**
 * 
 * @author devdemetri
 *
 */
public class Sequence {
		
	private String accession;
	private String definition;
	private Integer tax_id;
	private String organism;
	private String isolate;
	private String strain;
	private String collection_date; 
	private String comment;
	private String sequence;
	private Integer segment_length;
	private Publication pub;
	
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
	
	public Integer getTax_id() {
		return tax_id;
	}
	
	public void setTax_id(Integer tax_id) {
		this.tax_id = tax_id;
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
	
	public String getCollection_date() {
		return collection_date;
	}
	
	public void setCollection_date(String collection_date) {
		this.collection_date = collection_date;
	}
	
	public String getComment() {
		return comment;
	}
	
	public void setComment(String comment) {
		this.comment = comment;
	}
	
	public String getSequence() {
		return sequence;
	}
	
	public void setSequence(String sequence) {
		this.sequence = sequence;
	}
	
	public Integer getSegment_length() {
		return segment_length;
	}
	
	public void setSegment_length(Integer segment_length) {
		this.segment_length = segment_length;
	}
	
	public Publication getPub() {
		return pub;
	}
	
	public void setPub(Publication pub) {
		this.pub = pub;
	}

}
