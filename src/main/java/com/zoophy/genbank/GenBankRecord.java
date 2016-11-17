package com.zoophy.genbank;

import java.util.List;

import org.apache.lucene.document.Document;

import java.sql.ResultSet;
import java.util.LinkedList;

/**
 * Main object for representing GenBank records. This is a truncated version that has everything needed for these services.
 * @author devdemetri
 */
public class GenBankRecord {
	
	private String accession;
	private Sequence sequence;
	private List<Gene> genes;
	private Host host; 
	private Location geonameLocation;
	
	public GenBankRecord(Document luceneDoc) throws GenBankRecordException {
		try {
			genes = new LinkedList<Gene>();
			
		}
		catch (Exception e) {
			throw new GenBankRecordException("ERROR converting Lucene Document to GenBankRecord: "+e.getMessage());
		}
	}
	
	public GenBankRecord(ResultSet rs) throws GenBankRecordException {
		try {
			genes = new LinkedList<Gene>();
			
		}
		catch (Exception e) {
			throw new GenBankRecordException("ERROR converting ResultSet to GenBankRecord: "+e.getMessage());
		}
	}

	public String getAccession() {
		return accession;
	}

	public void setAccession(String accession) {
		this.accession = accession;
	}

	public Sequence getSequence() {
		return sequence;
	}

	public void setSequence(Sequence sequence) {
		this.sequence = sequence;
	}

	public List<Gene> getGenes() {
		return genes;
	}

	public void setGenes(List<Gene> genes) {
		this.genes = genes;
	}

	public Host getHost() {
		return host;
	}

	public void setHost(Host host) {
		this.host = host;
	}

	public Location getGeonameLocation() {
		return geonameLocation;
	}

	public void setGeonameLocation(Location geonameLocation) {
		this.geonameLocation = geonameLocation;
	}
	
}