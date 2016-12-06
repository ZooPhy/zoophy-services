package edu.asu.zoophy.index;

import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import edu.asu.zoophy.genbank.GenBankRecord;
import edu.asu.zoophy.genbank.Gene;
import edu.asu.zoophy.genbank.Host;
import edu.asu.zoophy.genbank.Location;
import edu.asu.zoophy.genbank.Publication;
import edu.asu.zoophy.genbank.Sequence;

/**
 * @author devdemetri
 * Maps Lucene Documents to Java Objects
 */
public class DocumentMapper {
	
	/**
	 * Maps Lucene Document to GenBankRecord 
	 * @param doc - Lucene Document
	 * @throws LuceneSearcherException
	 */
	public static GenBankRecord mapRecord(Document doc) throws LuceneSearcherException {
		GenBankRecord rec = new GenBankRecord();
		try {
			final String acc = doc.getField("Accession").stringValue();
			rec.setAccession(acc);
			Sequence seq = new Sequence();
			seq.setAccession(acc);
			if (doc.getField("Date") != null) {
				seq.setCollectionDate(doc.getField("Date").stringValue());
			}
			if (doc.getField("Definition") != null) {
				seq.setDefinition(doc.getField("Definition").stringValue());
			}
			if (doc.getField("Organism") != null) {
				seq.setOrganism(doc.getField("Organism").stringValue());
			}
			if (doc.getField("SegmentLength") != null) {
				seq.setSegmentLength(Integer.parseInt(doc.getField("SegmentLength").stringValue()));
			}
			if (doc.getField("Strain") != null) {
				seq.setStrain(doc.getField("Strain").stringValue());
			}
			if (doc.getFields("TaxonID") != null) {
				for (Field f : doc.getFields("TaxonID")) {
					try {
						seq.setTaxID(Integer.parseInt(f.stringValue()));
						break;
					}
					catch (Exception e) {
						//do nothing
					}
				}
			}
			rec.setSequence(seq);
			Location loc = new Location();
			loc.setAccession(acc);
			if (doc.getFields("GeonameID") != null) {
				for (Field f : doc.getFields("GeonameID")) {
					try {
						loc.setGeonameID(Long.parseLong(f.stringValue()));
						break;
					}
					catch (Exception e) {
						//do nothing
					}
				}
			}
			if (doc.getField("Location") != null) {
				loc.setLocation(doc.getField("Location").stringValue());
			}
			if (doc.getField("LocationType") != null) {
				loc.setGeonameType(doc.getField("LocationType").stringValue());
			}
			if (doc.getField("Latitude") != null) {
				loc.setLatitude(Double.parseDouble(doc.getField("Latitude").stringValue()));
			}
			if (doc.getField("Longitude") != null) {
				loc.setLongitude(Double.parseDouble(doc.getField("Longitude").stringValue()));
			}
			rec.setGeonameLocation(loc);
			Host host = new Host();
			host.setAccession(acc);
			if (doc.getField("Host Name") != null) {
				host.setName(doc.getField("Host Name").stringValue());
			}
			if (doc.getFields("HostID") != null) {
				for (Field f : doc.getFields("HostID")) {
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
				rec.setHost(host);
			}
			Publication pub = new Publication();
			if (doc.getField("PubmedID") != null && !doc.getField("PubmedID").stringValue().equalsIgnoreCase("n/a")) {
				pub.setPubMedID(Integer.parseInt(doc.getField("PubmedID").stringValue()));
			}
			if (doc.getFields("Gene").length > 0) {
				List<Gene> genes = rec.getGenes();
				for (Field f : doc.getFields("Gene")) {
					if (!(f.stringValue().equalsIgnoreCase("complet") || f.stringValue().equalsIgnoreCase("complete"))) {
						Gene g = new Gene();
						g.setAccession(acc);
						g.setName(f.stringValue());
						genes.add(g);
					}
				}
				rec.setGenes(genes);
			}
			return rec;
		}
		catch (Exception e) {
			throw new LuceneSearcherException("Failed to map document to record: "+e.getCause() + " : " + e.getMessage());
		}
	}
	
	
}
