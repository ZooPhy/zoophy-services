package edu.asu.zoophy.pipeline.utils;

import edu.asu.zoophy.genbank.Location;

/**
 * Static utility methods for normalizing names, locations, dates, etc.
 * @author devdemetri
 */
public class Normalizer {

	/**
	 * Normalizes location names
	 * @param location - Location to normalize
	 * @return normalized name of location
	 */
	protected static String normalizeLocation(Location location) {
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
	
}
