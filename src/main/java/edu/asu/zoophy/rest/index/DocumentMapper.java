package edu.asu.zoophy.rest.index;

import java.util.List;
import java.util.logging.Logger;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;

import edu.asu.zoophy.rest.genbank.GenBankRecord;
import edu.asu.zoophy.rest.genbank.Gene;
import edu.asu.zoophy.rest.genbank.Host;
import edu.asu.zoophy.rest.genbank.Location;
import edu.asu.zoophy.rest.genbank.Publication;
import edu.asu.zoophy.rest.genbank.Sequence;

/**
 * @author devdemetri
 * Maps Lucene Documents to Java Objects
 */
public class DocumentMapper {
	
	private static Logger log = Logger.getLogger("DocumentMapper");
	
	/**
	 * Maps Lucene Document to GenBankRecord 
	 * @param luceneDocument - Lucene Document to map
	 * @throws LuceneSearcherException
	 */
	public static GenBankRecord mapRecord(Document luceneDocument) throws LuceneSearcherException {
		GenBankRecord record = new GenBankRecord();
		try {
			final String recordAccession = luceneDocument.get("Accession");
			record.setAccession(recordAccession);
			Sequence sequence = new Sequence();
			sequence.setAccession(recordAccession);
			sequence.setCollectionDate(luceneDocument.get("Date"));
			sequence.setDefinition(luceneDocument.get("Definition"));
			sequence.setOrganism(luceneDocument.get("Organism"));
			sequence.setSegmentLength(Integer.parseInt(luceneDocument.get("SegmentLength")));
			sequence.setStrain(luceneDocument.get("Strain"));
			for (Fieldable field : luceneDocument.getFieldables("TaxonID")) {
				try {
					sequence.setTaxID(Integer.parseInt(field.stringValue()));
					break;
				}
				catch (Exception e) {
					log.warning("Could not parse TaxID: "+e.getMessage());
				}
			}
			sequence.setPH1N1(Boolean.valueOf(luceneDocument.get("PH1N1")));
			record.setSequence(sequence);
			Location location = new Location();
			location.setAccession(recordAccession);
			for (Fieldable field : luceneDocument.getFieldables("GeonameID")) {
				try {
					location.setGeonameID(Long.parseLong(field.stringValue()));
					break;
				}
				catch (Exception e) {
					log.warning("Could not parse GeonameID: "+e.getMessage());
				}
			}
			location.setLocation(luceneDocument.get("Location"));
			location.setGeonameType(luceneDocument.get("LocationType"));
			if (luceneDocument.get("Latitude") != null) {
				location.setLatitude(Double.parseDouble(luceneDocument.get("Latitude")));
			}
			if (luceneDocument.get("Longitude") != null) {
				location.setLongitude(Double.parseDouble(luceneDocument.get("Longitude")));
			}
			location.setCountry(luceneDocument.get("Country"));
			record.setGeonameLocation(location);
			Host host = new Host();
			host.setAccession(recordAccession);
			host.setName(luceneDocument.get("Host_Name"));
			for (Fieldable field : luceneDocument.getFieldables("HostID")) {
				try {
					host.setTaxon(Integer.parseInt(field.stringValue()));
					break;
				}
				catch (Exception e) {
					log.warning("Could not parse HostID: "+e.getMessage());
				}
			}
			if (host.getTaxon() != null && host.getTaxon() != 1) {
				record.setHost(host);
			}
			Publication publication = new Publication();
			if (luceneDocument.get("PubmedID") != null && !luceneDocument.get("PubmedID").equalsIgnoreCase("n/a")) {
				publication.setPubMedID(Integer.parseInt(luceneDocument.get("PubmedID")));
			}
			if (luceneDocument.getFieldables("Gene").length > 0) {
				List<Gene> genes = record.getGenes();
				boolean isComplete = false;
				for (Fieldable field : luceneDocument.getFieldables("Gene")) {
					if (field.stringValue().equalsIgnoreCase("Complete")) {
						isComplete = true;
					}
					Gene gene = new Gene();
					gene.setAccession(recordAccession);
					gene.setName(field.stringValue());
					genes.add(gene);
				}
				if (isComplete) {
					genes.clear();
					Gene gene = new Gene();
					gene.setAccession(recordAccession);
					gene.setName("Complete");
					genes.add(gene);
				}
				record.setGenes(genes);
			}
			return record;
		}
		catch (Exception e) {
			throw new LuceneSearcherException("Failed to map document to record: "+e.getCause() + " : " + e.getMessage());
		}
	}
	
}
