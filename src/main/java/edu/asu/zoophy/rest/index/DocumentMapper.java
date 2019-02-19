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
			sequence.setDate(luceneDocument.get("Date"));
			sequence.setCollectionDate(luceneDocument.get("NormalizedDate"));
			sequence.setDefinition(luceneDocument.get("Definition"));
			sequence.setOrganism(luceneDocument.get("Organism"));
			sequence.setSegmentLength(Integer.parseInt(luceneDocument.get("SegmentLength")));
			sequence.setStrain(luceneDocument.get("Strain"));
			try {
				sequence.setTaxID(Integer.parseInt(SplitString(luceneDocument.get("OrganismID"))));
			}
			catch (Exception e) {
				log.warning("Could not parse OrganismID: "+e.getMessage());
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
			location.setLocation(SimplifyCountry(luceneDocument.get("Location")));
			location.setGeonameType(luceneDocument.get("LocationType"));
			if (luceneDocument.get("Latitude") != null) {
				location.setLatitude(Double.parseDouble(luceneDocument.get("Latitude")));
			}
			if (luceneDocument.get("Longitude") != null) {
				location.setLongitude(Double.parseDouble(luceneDocument.get("Longitude")));
			}
			location.setCountry(luceneDocument.get("Country"));
			location.setState(luceneDocument.get("State"));
			record.setGeonameLocation(location);
			Host host = new Host();
			host.setAccession(recordAccession);
			host.setName(luceneDocument.get("HostNormalizedName"));
			try {
				host.setTaxon(Integer.parseInt(SplitString(luceneDocument.get("HostID"))));
			}
			catch (Exception e) {
				log.warning("Could not parse HostID: "+e.getMessage());
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
	
	private static String SplitString(String organismList) {
		String[] organisms = organismList.split(" ");
		if(organisms.length > 0) {
			return organisms[0];
		}
		return "";
	}
	
	private static String SimplifyCountry(String country_name) {
		if (country_name != null) {
			if (country_name.contains("Great Britain")) {
				country_name = "United Kingdom";
			}
			else if (country_name.equalsIgnoreCase("Russian Federation")) {
				country_name = "Russia";
			}
			else if (country_name.equalsIgnoreCase("Repubblica Italiana")) {
				country_name = "Italy";
			}
			else if (country_name.equalsIgnoreCase("Polynésie Française")) {
				country_name = "French Polynesia";
			}
			else if (country_name.equalsIgnoreCase("Lao People’s Democratic Republic")) {
				country_name = "Laos";
			}
			else if (country_name.equalsIgnoreCase("Argentine Republic")){
				country_name = "Argentina";
			}
			else if (country_name.equalsIgnoreCase("Portuguese Republic")){
				country_name = "Portugal";
			}
			else {
				if (country_name.contains("Republic of ")) {
					country_name = country_name.substring(country_name.indexOf("Republic of ")+12);
					if (country_name.contains("the ")) {
						country_name = country_name.substring(country_name.indexOf("the ")+4);
					}
				}
				else if (country_name.contains("Kingdom of ")) {
					country_name = country_name.substring(country_name.indexOf("Kingdom of ")+11);
					if (country_name.contains("the ")) {
						country_name = country_name.substring(country_name.indexOf("the ")+4);
					}
				}
				else if (country_name.contains("Union of ")) {
					country_name = country_name.substring(country_name.indexOf("Union of ")+9);
					if (country_name.contains("the ")) {
						country_name = country_name.substring(country_name.indexOf("the ")+4);
					}
				}
				else if (country_name.contains("State of ")) {
					country_name = country_name.substring(country_name.indexOf("State of ")+9);
					if (country_name.contains("the ")) {
						country_name = country_name.substring(country_name.indexOf("the ")+4);
					}
				}
				else if (country_name.contains("Commonwealth of ")) {
					country_name = country_name.substring(country_name.indexOf("Commonwealth of ")+16);
					if (country_name.contains("the ")) {
						country_name = country_name.substring(country_name.indexOf("the ")+4);
					}
				}
				else if (country_name.endsWith("Special Administrative Region")) {
					country_name = country_name.substring(0,country_name.indexOf("Special Administrative Region")-1);
				}
			}
		}
		else {
			country_name = "Unknown";
		}
		return country_name;
	}
	
}
