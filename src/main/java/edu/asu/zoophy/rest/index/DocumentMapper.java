package edu.asu.zoophy.rest.index;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

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
	
	private final static Logger log = Logger.getLogger("DocumentMapper");
	
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
			for (IndexableField field : luceneDocument.getFields("TaxonID")) {
				if (field.stringValue().matches("[0-9]{1,12}+")) {
					try {
						sequence.setTaxID(Integer.parseInt(field.stringValue()));
						break;
					}
					catch (Exception e) {
						log.warning("Could not parse TaxID: "+e.getMessage());
					}
				}
			}
			sequence.setPH1N1(Boolean.valueOf(luceneDocument.get("PH1N1")));
			record.setSequence(sequence);
			Location location = new Location();
			location.setAccession(recordAccession);
			for (IndexableField field : luceneDocument.getFields("GeonameID")) {
				if (field.stringValue().matches("[0-9]{1,12}+")) {
					try {
						location.setGeonameID(Long.parseLong(field.stringValue()));
						break;
					}
					catch (Exception e) {
						log.warning("Could not parse GeonameID: "+e.getMessage());
					}
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
			for (IndexableField field : luceneDocument.getFields("HostID")) {
				if (field.stringValue().matches("[0-9]{1,12}+")) {
					try {
						host.setTaxon(Integer.parseInt(field.stringValue()));
						break;
					}
					catch (Exception e) {
						log.warning("Could not parse HostID: "+e.getMessage());
					}
				}
			}
			if (host.getTaxon() != null && host.getTaxon() != 1) {
				record.setHost(host);
			}
			Publication publication = new Publication();
			if (luceneDocument.get("PubmedID") != null && !luceneDocument.get("PubmedID").equalsIgnoreCase("n/a")) {
				publication.setPubMedID(Integer.parseInt(luceneDocument.get("PubmedID")));
			}
			if (luceneDocument.getFields("Gene").length > 0) {
				List<Gene> genes = record.getGenes();
				Set<String> uniqueGenes = new HashSet<String>(8);
				boolean isComplete = false;
				for (IndexableField field : luceneDocument.getFields("Gene")) {
					String geneName = field.stringValue();
					if (geneName.equalsIgnoreCase("Complete")) {
						isComplete = true;
					}
					if (!uniqueGenes.contains(geneName)) {
						uniqueGenes.add(geneName);
						Gene gene = new Gene();
						gene.setAccession(recordAccession);
						gene.setName(field.stringValue());
						genes.add(gene);
					}
				}
				uniqueGenes.clear();
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
