package edu.asu.zoophy.pipeline.utils;

import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

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
					break;
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
			for (String p : parts) {
				date += p;
			}
			return date;
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "ERROR! could not format date: "+collectionDate+" : "+e.getMessage());
			throw new NormalizerException("Error formatting date: "+e.getMessage(), null);
		}
	}

	
}
