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
			location.setGeonameID(Long.parseLong(luceneDocument.get("GeonameId")));
			location.setLocation(formatLocationName(luceneDocument.get("Name")));
			location.setGeonameType(luceneDocument.get("Code"));
			if (luceneDocument.get("Latitude") != null) {
				location.setLatitude(Double.parseDouble(luceneDocument.get("Latitude")));
			}
			if (luceneDocument.get("Longitude") != null) {
				location.setLongitude(Double.parseDouble(luceneDocument.get("Longitude")));
			}
			location.setCountry(formatLocationName(luceneDocument.get("Country")));
			location.setState(formatLocationName(luceneDocument.get("State")));
			location.setHierarchy(formatLocationName(luceneDocument.get("AncestorsNames")));
			location.setPopulation(Long.parseLong(luceneDocument.get("Population")));
			return location;
		}
		catch (Exception e) {
			throw new LuceneSearcherException("Failed to map document to record: "+ luceneDocument.toString() + " -> " + e.getMessage());
		}
	}

	public static String formatLocationName(String fieldValue){
		if (fieldValue != null) {
			fieldValue = fieldValue.replaceAll(" *\\([^)]*\\) *", "");
		}
		return fieldValue;
	}
}
