package edu.asu.zoophy.rest.index;

import java.util.logging.Logger;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import edu.asu.zoophy.rest.genbank.Location;

/**
 * @author amagge
 * Maps Lucene Documents to Java Objects
 */
public class GeonamesDocumentMapper {
	
	private final static Logger log = Logger.getLogger("GeonamesDocumentMapper");
	
	/**
	 * Maps Lucene Document to Location object 
	 * @param luceneDocument - Lucene Document to map
	 * @throws LuceneSearcherException
	 */
	public static Location mapRecord(Document luceneDocument) throws LuceneSearcherException {
		Location location = new Location();
		try {
			location.setAccession("temp"); //set it to empty and replace later
			for (IndexableField field : luceneDocument.getFields("GeonameId")) {
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
			location.setLocation(luceneDocument.get("Name"));
			location.setGeonameType(luceneDocument.get("Code"));
			if (luceneDocument.get("Latitude") != null) {
				location.setLatitude(Double.parseDouble(luceneDocument.get("Latitude")));
			}
			if (luceneDocument.get("Longitude") != null) {
				location.setLongitude(Double.parseDouble(luceneDocument.get("Longitude")));
			}
			location.setCountry(luceneDocument.get("Country"));
			location.setState(luceneDocument.get("State"));
			location.setHierarchy(luceneDocument.get("AncestorName"));
			location.setPopulation(Long.parseLong(luceneDocument.get("Population")));
			return location;
		}
		catch (Exception e) {
			throw new LuceneSearcherException("Failed to map document to record: "+e.getCause() + " : " + e.getMessage());
		}
	}
	
}
