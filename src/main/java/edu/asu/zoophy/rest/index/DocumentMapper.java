package edu.asu.zoophy.rest.index;

import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

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
	
	/**
	 * Maps Lucene Document to GenBankRecord 
	 * @param luceneDocument - Lucene Document to map
	 * @throws LuceneSearcherException
	 */
	public static GenBankRecord mapRecord(Document luceneDocument) throws LuceneSearcherException {
		GenBankRecord record = new GenBankRecord();
		try {
			final String recordAccession = luceneDocument.getField("Accession").stringValue();
			record.setAccession(recordAccession);
			Sequence sequence = new Sequence();
			sequence.setAccession(recordAccession);
			if (luceneDocument.getField("Date") != null) {
				sequence.setCollectionDate(luceneDocument.getField("Date").stringValue());
			}
			if (luceneDocument.getField("Definition") != null) {
				sequence.setDefinition(luceneDocument.getField("Definition").stringValue());
			}
			if (luceneDocument.getField("Organism") != null) {
				sequence.setOrganism(luceneDocument.getField("Organism").stringValue());
			}
			if (luceneDocument.getField("SegmentLength") != null) {
				sequence.setSegmentLength(Integer.parseInt(luceneDocument.getField("SegmentLength").stringValue()));
			}
			if (luceneDocument.getField("Strain") != null) {
				sequence.setStrain(luceneDocument.getField("Strain").stringValue());
			}
			if (luceneDocument.getFields("TaxonID") != null) {
				for (Field field : luceneDocument.getFields("TaxonID")) {
					try {
						sequence.setTaxID(Integer.parseInt(field.stringValue()));
						break;
					}
					catch (Exception e) {
						//do nothing
					}
				}
			}
			record.setSequence(sequence);
			Location location = new Location();
			location.setAccession(recordAccession);
			if (luceneDocument.getFields("GeonameID") != null) {
				for (Field field : luceneDocument.getFields("GeonameID")) {
					try {
						location.setGeonameID(Long.parseLong(field.stringValue()));
						break;
					}
					catch (Exception e) {
						//do nothing
					}
				}
			}
			if (luceneDocument.getField("Location") != null) {
				location.setLocation(luceneDocument.getField("Location").stringValue());
			}
			if (luceneDocument.getField("LocationType") != null) {
				location.setGeonameType(luceneDocument.getField("LocationType").stringValue());
			}
			if (luceneDocument.getField("Latitude") != null) {
				location.setLatitude(Double.parseDouble(luceneDocument.getField("Latitude").stringValue()));
			}
			if (luceneDocument.getField("Longitude") != null) {
				location.setLongitude(Double.parseDouble(luceneDocument.getField("Longitude").stringValue()));
			}
			if (luceneDocument.getField("Country") != null) {
				location.setCountry(luceneDocument.getField("Country").stringValue()); 
			}
			record.setGeonameLocation(location);
			Host host = new Host();
			host.setAccession(recordAccession);
			if (luceneDocument.getField("Host_Name") != null) {
				host.setName(luceneDocument.getField("Host_Name").stringValue());
			}
			if (luceneDocument.getFields("HostID") != null) {
				for (Field f : luceneDocument.getFields("HostID")) {
					try {
						host.setTaxon(Integer.parseInt(f.stringValue()));
						break;
					}
					catch (Exception e) {
						//do nothing
					}
				}
			}
			if (host.getTaxon() != null && host.getTaxon() != 1) {
				record.setHost(host);
			}
			Publication publication = new Publication();
			if (luceneDocument.getField("PubmedID") != null && !luceneDocument.getField("PubmedID").stringValue().equalsIgnoreCase("n/a")) {
				publication.setPubMedID(Integer.parseInt(luceneDocument.getField("PubmedID").stringValue()));
			}
			if (luceneDocument.getFields("Gene").length > 0) {
				List<Gene> genes = record.getGenes();
				boolean isComplete = false;
				for (Field field : luceneDocument.getFields("Gene")) {
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
