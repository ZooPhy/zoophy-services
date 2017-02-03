package edu.asu.zoophy.rest.genbank;

import java.util.List;
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
	private Publication publication;
	
	public GenBankRecord() {
		genes = new LinkedList<Gene>();
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

	public Publication getPublication() {
		return publication;
	}

	public void setPublication(Publication publication) {
		this.publication = publication;
	}
	
}