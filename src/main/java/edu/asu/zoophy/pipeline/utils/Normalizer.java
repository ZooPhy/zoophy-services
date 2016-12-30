package edu.asu.zoophy.pipeline.utils;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.asu.zoophy.genbank.Gene;
import edu.asu.zoophy.genbank.Location;

/**
 * Static utility methods for normalizing names, locations, dates, etc.
 * @author devdemetri
 */
public class Normalizer {

	private final static DecimalFormat df4 = new DecimalFormat(".####");
	private static Logger log = Logger.getLogger("Normalizer");
	
	/**
	 * Normalizes location names
	 * @param location - Location to normalize
	 * @return normalized name of location
	 */
	public static String normalizeLocation(Location location) {
		try {
			if (location.getLocation() != null) {
				String loc = location.getLocation().trim();
				if (loc.contains(",")) {
					loc  = loc.split(",")[0];
				}
				if (loc.contains(".")) {
					loc  = loc.replaceAll(".", "-");
				}
				if (loc.contains("_")) {
					loc  = loc.replaceAll("_", "-");
				}
				if (loc.contains(" ")) {
					loc  = loc.replaceAll(" ", "-");
				}
				if (loc.trim().isEmpty()) {
					return "unknown";
				}
				else {
					return loc.trim().toLowerCase();
				}
			}
			else {
				return "unknown";
			}
		}
		catch (Exception e) {
			return "unknown";
		}
	}
	
	/**
	 * Converts a Text Date into a Decimal Date ex.
	 * @param date - date to be converted to decimal format
	 * @return date in decimal format
	 * @throws NormalizerException 
	 */
	public static String dateToDecimal(String date) throws NormalizerException {
		try {
			double day = Double.parseDouble(date.substring(0, 2));
			String month = date.substring(2, 5);
			switch (month) {
				case "Dec":
					day += 30.0;
				case "Nov":
					day += 31.0;
				case "Oct":
					day += 30.0;
				case "Sep":
					day += 31.0;
				case "Aug":
					day += 31.0;
				case "Jul":
					day += 30.0;
				case "Jun":
					day += 31.0;
				case "May":
					day += 30.0;
				case "Apr":
					day += 31.0;
				case "Mar":
					day += 28.0;
				case "Feb":
					day += 31.0;
			}
			day = day/365.0;
			String decimalDate =  df4.format(Integer.parseInt(date.substring(5))+day);
			return decimalDate;
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "ERROR! could not convert date to decimal: "+date+" : "+e.getMessage());
			throw new NormalizerException("Error converting date to decimal: "+e.getMessage(), null);
		}
	}
	
	public static String formatDate(String collectionDate) throws NormalizerException {
		try {
			String[] parts = collectionDate.split("-");
			String date = "";
			if (parts.length < 3) {
				date += "01";
			}
			if (parts.length < 2) {
				date += "Jan"; 
			}
			for (String part : parts) {
				date += part;
			}
			return date;
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "ERROR! could not format date: "+collectionDate+" : "+e.getMessage());
			throw new NormalizerException("Error formatting date: "+e.getMessage(), null);
		}
	}
	
	/**
	 * Converts a list of Genes to a string for the CSV format
	 * @param genes
	 * @return CSV safe string for the list of Genes
	 */
	public static String geneListToCSVString(List<Gene> genes) {
		if (genes != null) {
			List<String> geneStrings = new LinkedList<String>();
			for (Gene gene : genes) {
				if (gene.getName() != null) {
					if (gene.getName().equalsIgnoreCase("Complete")) {
						return "Complete";
					}
					geneStrings.add(gene.getName());
				}
			}
			Collections.sort(geneStrings);
			String result = "None";
			if (geneStrings.size() > 0) {
				result = geneStrings.get(0).toUpperCase();
				geneStrings.remove(0);
				for (String gene : geneStrings) {
					result += " "+gene;
				}
			}
			geneStrings.clear();
			return result;
		}
		return "None";
	}
	
	/**
	 * Normalizes Organism names
	 * @param organism
	 * @return
	 */
	public static String simplifyOrganism(String organism) {
		if (organism != null) {
			int cutoff = organism.indexOf(";");
			if (cutoff == -1 || (organism.indexOf("/") < cutoff && organism.indexOf("/") > 0)) {
				cutoff = organism.indexOf("/");
			}
			if (cutoff == -1 || (organism.indexOf("Viruses") < cutoff && organism.indexOf("Viruses") > 0)) {
				cutoff = organism.indexOf("Viruses");
			}
			if (cutoff == -1 || (organism.indexOf(" virus") < cutoff && organism.indexOf(" virus") > 0)) {
				cutoff = organism.indexOf(" virus");
			}
			if (cutoff > 0) {
				return organism.substring(0, cutoff);
			}
		}
		return organism;
	}

	/**
	 * Normalizes Dates
	 * @param date
	 * @return Normalized Date
	 */
	public static String normalizeDate(String date) {
		if (date != null && !date.equals("10000101")) {
			if (date.length() == 8) {
				String month = getMonthName(date.substring(4,6));
				return  date.substring(6) + "-" + month + "-" + date.substring(0,4);
			}
			else if (date.length() == 6) {
				String month = getMonthName(date.substring(4));
				return month + "-" + date.substring(0,4);
			}
			else if (date.length() == 4) {
				return date;
			}
			return date;
		}
		else {
			return "Unknown";
		}
	}
	
	/**
	 * Converts Month Number to Month Name
	 * @param monthNumber
	 * @return Name of the Month for the given Month Number
	 */
	public static String getMonthName(String monthNumber) {
		switch (Integer.parseInt(monthNumber)) {
		case 1:
			return "Jan";
		case 2:
			return "Feb";
		case 3: 
			return "Mar";
		case 4: 
			return "Apr"; 
		case 5:
			return "May"; 
		case 6:
			return "Jun";
		case 7:
			return "Jul"; 
		case 8:
			return "Aug"; 
		case 9: 
			return "Sep";
		case 10: 
			return "Oct"; 
		case 11:
			return "Nov";
		case 12:
			return "Dec"; 
		default:
			return "__"; 
		}
	}
	
	/**
	 * Converts a raw String into a CSV safe String
	 * @param raw
	 * @return
	 */
	public static String csvify(String raw) {
		if (raw == null) {
			return "Unknown";
		}
		else {
			return raw.replaceAll(",", " ");
		}
	}

	
}
