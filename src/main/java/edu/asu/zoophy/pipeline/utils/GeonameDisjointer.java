package edu.asu.zoophy.pipeline.utils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import edu.asu.zoophy.genbank.GenBankRecord;
import edu.asu.zoophy.genbank.Location;
import edu.asu.zoophy.index.LuceneSearcher;
import edu.asu.zoophy.index.LuceneSearcherException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;


/**
 * Responsible for making sure ZooPhy job locations are disjoint
 * @author devdemetri
 */
public class GeonameDisjointer {
	
	@Autowired
	private LuceneSearcher indexSearcher;
	
	@Autowired
	private Environment env;

	private static final GeoHierarchy hierarchy = GeoHierarchy.getInstance();
	
	private final int MAX_STATES = Integer.parseInt(env.getProperty("job.max.locations"));
	
	private Map<String,Set<Long>> ancestors;
	
	public GeonameDisjointer() {
		ancestors = new HashMap<String,Set<Long>>();
	}
	
	public List<GenBankRecord> disjointRecords(List<GenBankRecord> recordsToCheck) throws DisjointerException {
		
		Map<Long,Long> disjoins = new HashMap<Long,Long>();
		Set<Location> locations = new LinkedHashSet<Location>();
		Map<String,Integer> types = new LinkedHashMap<String,Integer>();
		Set<Location> locationsToRemove;
		Map<Long,String> idToLocation = new HashMap<Long,String>();
		String commonType = null;
		int maxType = 0;
		
		for (int i = 0; i < recordsToCheck.size(); i++) {
			GenBankRecord record = recordsToCheck.get(i);
			if (record.getGeonameLocation() == null || Normalizer.normalizeLocation(record.getGeonameLocation()).equalsIgnoreCase("unknown") || record.getGeonameLocation().getGeonameType() == null) {
				recordsToCheck.remove(i);
			}
			else {
				String type = record.getGeonameLocation().getGeonameType();
				if (types.get(type) == null) {
					types.put(type, 0);
				}
				types.put(type, (types.get(type)+1));
				Set<Long> recordAncestors;
				try {
					recordAncestors = indexSearcher.findLocationAncestors(record.getAccession());
				}
				catch (LuceneSearcherException lse) {
					throw new DisjointerException("Error retrieving location ancestors: "+lse.getMessage(), "Error Disjointing Locations");
				}
				recordAncestors.remove(record.getGeonameLocation().getGeonameID());
				ancestors.put(record.getAccession(),recordAncestors);
			}
		}
		for (String type : types.keySet()) {
			if (types.get(type) > maxType) {
				maxType = types.get(type);
				commonType = type;
			}
		}
		for (int i = 0; i < recordsToCheck.size(); i++) {
			GenBankRecord record = recordsToCheck.get(i);
			Location recordLocation = record.getGeonameLocation();
			boolean isDisjoint = true;
			if (hierarchy.isParent(commonType, record.getGeonameLocation().getGeonameType())) {
				isDisjoint = false;
				recordsToCheck.remove(i);
			}
			else {
				for (Location parent : locations) {
					if (isParent(parent,recordLocation)) {
						isDisjoint = false;
						if (!parent.getGeonameID().equals(recordLocation.getGeonameID())) {
							disjoins.put(recordLocation.getGeonameID(),parent.getGeonameID());
						}
						break;
					}
				}
			}
			if (isDisjoint) {
				locations.add(recordLocation);
			}
		}
		locationsToRemove = new HashSet<Location>();
		for (Location location : locations) {
			for (Location locationParent : locations) {
				if (!(locationParent.getGeonameID().equals(location.getGeonameID()) || locationsToRemove.contains(locationParent))) {
					if (isParent(locationParent,location)) {
						if (!location.getGeonameID().equals(locationParent.getGeonameID())) {
							disjoins.put(location.getGeonameID(),locationParent.getGeonameID());
						}
						locationsToRemove.add(location);
						break;
					}
				}
			}
		}
		locations.removeAll(locationsToRemove);
		locationsToRemove.clear();
		if (locations.size() < 2) {
			String userErr = "Too few distinct locations (need at least 2): " + locations.size();
			if (locations.size() == 1) {
				userErr += "\nLocation: "+locations.iterator().next().getLocation();
			}
			throw new DisjointerException("Too few distinct locations: "+locations.size(),userErr);
		}
		else if (locations.size() > MAX_STATES) {
			StringBuilder userErr = new StringBuilder("Too many distinct locations (limit is "+MAX_STATES+"): " + locations.size());
			userErr.append("\nLocations: ");
			for (Location location : locations) {
				userErr.append("\n\t");
				userErr.append(location.getLocation());
			}
			throw new DisjointerException("Too many distinct locations: "+locations.size(), userErr.toString());
		}
		for (Location location : locations) {
			idToLocation.put(location.getGeonameID(), location.getLocation());
		}
		for (GenBankRecord record : recordsToCheck) {
			Long tempGeonameID = record.getGeonameLocation().getGeonameID();
			if (disjoins.get(tempGeonameID) != null) {
				Long disjointID = null;
				while (disjoins.get(tempGeonameID) != null) {
					if (disjoins.get(tempGeonameID) != null) {
						disjointID = disjoins.get(tempGeonameID);
					}
					tempGeonameID = disjoins.get(tempGeonameID);
				}
				String newLoc = idToLocation.get(disjointID);
				setLocationName(record, newLoc);
			}
		}
		idToLocation.clear();
		return recordsToCheck;
	}
	
	/**
	 * 
	 * @param locationParent
	 * @param location
	 * @return
	 */
	private boolean isParent(Location locationParent, Location location) {
	    Set<Long> locationAncestors = ancestors.get(location.getAccession());
		if (location.getGeonameID().equals(locationParent.getGeonameID()) || locationAncestors.contains(locationParent.getGeonameID())) {
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @param record
	 * @param locationName
	 */
	private void setLocationName(GenBankRecord record, String locationName) {
		Location recordLocation = record.getGeonameLocation();
		recordLocation.setLocation(locationName);
		record.setGeonameLocation(recordLocation);
	}
	
}
