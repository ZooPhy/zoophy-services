package edu.asu.zoophy.rest.pipeline.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import edu.asu.zoophy.rest.genbank.ExcludedRecords;
import edu.asu.zoophy.rest.genbank.GenBankRecord;
import edu.asu.zoophy.rest.genbank.InvalidRecords;
import edu.asu.zoophy.rest.genbank.Location;
import edu.asu.zoophy.rest.genbank.JobRecords;
import edu.asu.zoophy.rest.index.LuceneHierarchySearcher;
import edu.asu.zoophy.rest.index.LuceneSearcherException;
import edu.asu.zoophy.rest.pipeline.PipelineException;
import edu.asu.zoophy.rest.pipeline.PropertyProvider;
import edu.asu.zoophy.rest.pipeline.glm.GLMException;

/**
 * Responsible for making sure ZooPhy job locations are disjoint
 * @author devdemetri, kbhangal
 */
public class GeonameDisjoiner {

	private final LuceneHierarchySearcher hierarchyIndexSearcher;
	private final GeoHierarchy hierarchy = GeoHierarchy.getInstance();
	private final int MAX_STATES;
	private Map<Long, String> US_STATES;
	private Map<String, String> adminLevel;
	private final long BAD_DISJOIN = -1L;
	private Map<String,Set<Long>> ancestors = null;
	private Iterator<GenBankRecord> recordIter = null;
	private final Logger log;
	
	public GeonameDisjoiner(LuceneHierarchySearcher hierarchyIndexSearcher) throws PipelineException {
		this.hierarchyIndexSearcher = hierarchyIndexSearcher;
		PropertyProvider provider = PropertyProvider.getInstance();
		adminLevel = setupAdminLevelMap();
		MAX_STATES = Integer.parseInt(provider.getProperty("job.max.locations"));
		log = Logger.getLogger("GeonameDisjoiner");
	}
	
	/**
	 * Disjoins given list of records, filtering out records with invalid or too general locations. Remaining locations are normalized to a set of disjoint locations.
	 * @param recordsToCheck - list of records to Disjoin
	 * @return Filtered records with valid, disjoint Geoname locations.=
	 * @throws DisjoinerException
	 * @throws GLMException 
	 * @throws GeoHierarchyException 
	 */
	public JobRecords disjoinRecords(List<GenBankRecord> recordsToCheck) throws DisjoinerException, GLMException, GeoHierarchyException {
		List<ExcludedRecords> missingLocationRecords = new LinkedList<>();
		List<ExcludedRecords> higherAdminRecords = new LinkedList<>();
		try {
			Map<Long,Long> disjoins = new HashMap<Long,Long>((int)(recordsToCheck.size()*.75)+1);
			Set<Location> locations = new LinkedHashSet<Location>(50);
			Map<String,Set<Long>> types = new LinkedHashMap<String,Set<Long>>();
			Set<Location> locationsToRemove;
			Map<Long,String> idToLocation = new HashMap<Long,String>(50);
			ancestors = new HashMap<String,Set<Long>>((int)(recordsToCheck.size())+1, 1.0f);
			String commonType = null;
			int maxType = 0;
			try {
				recordIter = recordsToCheck.listIterator();
				while (recordIter.hasNext()) {
					GenBankRecord record = recordIter.next();
					if (record.getGeonameLocation() == null || Normalizer.normalizeLocation(record.getGeonameLocation()).equalsIgnoreCase("unknown") || record.getGeonameLocation().getGeonameType() == null) {
						missingLocationRecords.add(new ExcludedRecords(record.getAccession(), null));
						recordIter.remove();
					}
					else {
						Set<Long> recordAncestors;
						try {
							recordAncestors = hierarchyIndexSearcher.findLocationAncestors(record.getGeonameLocation().getGeonameID().toString());
							if (recordAncestors == null) {
								higherAdminRecords.add(new ExcludedRecords(record.getAccession(), adminCodeToCommonName(record.getGeonameLocation().getGeonameType())));
								recordIter.remove();
							}
							else {
								recordAncestors.remove(record.getGeonameLocation().getGeonameID());
								if (recordAncestors.isEmpty()) {
									higherAdminRecords.add(new ExcludedRecords(record.getAccession(), adminCodeToCommonName(record.getGeonameLocation().getGeonameType())));
									recordIter.remove();
								} else {
									String type = record.getGeonameLocation().getGeonameType();								
									if (types.get(type) == null) {
										Set<Long> geonameIds = new HashSet<Long>();
										types.put(type, geonameIds);
									}
									Set<Long> geonameIds = types.get(type);
									geonameIds.add(record.getGeonameLocation().getGeonameID());
									types.put(type, geonameIds);
									ancestors.put(record.getAccession(), recordAncestors);
								}
							}
						}
						catch (LuceneSearcherException lse) {
							throw new DisjoinerException("Error retrieving location ancestors: "+lse.getMessage(), "Error Disjoining Locations");
						}
					}
				}
				recordIter = null;
			}
			catch (PipelineException pe) {
				throw pe;
			}
			catch (Exception e) {
				throw new DisjoinerException("Error initially screening record locations:\t"+e.getMessage(), "Error Filtering Locations");
			}
			for (String type : types.keySet()) {
				if (types.get(type).size() > maxType) {
					maxType = types.get(type).size();
					commonType = type;
				}
			}
			try {
				recordIter = recordsToCheck.listIterator();
				while (recordIter.hasNext()) {
					GenBankRecord record = recordIter.next();
					Location recordLocation = record.getGeonameLocation();
					boolean isDisjoint = true;
					//selected record should be same or lower level than common level
					if (hierarchy.isParent(commonType, record.getGeonameLocation().getGeonameType())) {
						isDisjoint = false;
						recordIter.remove();
						higherAdminRecords.add(new ExcludedRecords(record.getAccession(), adminCodeToCommonName(record.getGeonameLocation().getGeonameType())));
					}
					else {
						for (Location parent : locations) {
							if (isAncestor(parent, recordLocation)) {
								isDisjoint = false;
								if (!parent.getGeonameID().equals(recordLocation.getGeonameID())) {
									disjoins.put(recordLocation.getGeonameID(), parent.getGeonameID());
								}
								break;
							}
						}
					}
					if (isDisjoint) {
						locations.add(recordLocation);
					}
				}
				recordIter = null;
			}
			catch (GeoHierarchyException ghe) {
				throw new GeoHierarchyException(ghe.getMessage(), "Error Filtering Locations");
			}
			catch (PipelineException pe) {
				throw pe;
			}
			catch (Exception e) {
				throw new DisjoinerException("Error filtering out locations above Common Type:\t"+e.getMessage(), "Error Filtering Locations");
			}
			locationsToRemove = new HashSet<Location>();
			try {
				for (Location location : locations) {
					for (Location locationParent : locations) {
						if (!(locationParent.getGeonameID().equals(location.getGeonameID()) || locationsToRemove.contains(locationParent))) {
							if (isAncestor(locationParent,location)) {
								if (!location.getGeonameID().equals(locationParent.getGeonameID())) {
									disjoins.put(location.getGeonameID(), locationParent.getGeonameID());
								}
								locationsToRemove.add(location);
								break;
							}
						}
					}
				}
				locations.removeAll(locationsToRemove);
			}
			catch (PipelineException pe) {
				throw pe;
			}
			catch (Exception e) {
				throw new DisjoinerException("Error removing overlapping locations:\t"+e.getMessage(), "Error Disjoining Locations");
			}
			locationsToRemove.clear();
			log.info("Distinct locations: "+locations.size());
			if (locations.size() < 2) {
				String userErr = "Too few distinct locations (need at least 2): " + locations.size();
				if (locations.size() == 1) {
					userErr += "\nLocation: "+locations.iterator().next().getLocation();
				}
				throw new DisjoinerException("Too few distinct locations: "+locations.size(),userErr);
			}
			else if (locations.size() > MAX_STATES) {
				StringBuilder userErr = new StringBuilder("Too many distinct locations (limit is "+MAX_STATES+"): " + locations.size());
				userErr.append("\nLocations: ");
				for (Location location : locations) {
					userErr.append("\n\t");
					userErr.append(location.getLocation());
				}
				throw new DisjoinerException("Too many distinct locations: "+locations.size(), userErr.toString());
			}
			try {
				for (Location location : locations) {
					idToLocation.put(location.getGeonameID(), location.getLocation());
				}
				recordIter = recordsToCheck.listIterator();
				while (recordIter.hasNext()) {
					GenBankRecord record = recordIter.next();
					Long tempGeonameID = record.getGeonameLocation().getGeonameID();
					if (disjoins.get(tempGeonameID) != null) {
						if (disjoins.get(tempGeonameID).longValue() == BAD_DISJOIN) {
							recordIter.remove();
							higherAdminRecords.add(new ExcludedRecords(record.getAccession(), adminCodeToCommonName(record.getGeonameLocation().getGeonameType())));
						}
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
				recordIter = null;
			}
			catch (Exception e) {
				throw new DisjoinerException("Error updating record locations to disjoint locations:\t"+e.getMessage(), "Error Disjoining Locations");
			}
			idToLocation.clear();
			
			List<InvalidRecords> invalidRecords = new LinkedList<>();
			if(!missingLocationRecords.isEmpty()) {
				invalidRecords.add(new InvalidRecords(missingLocationRecords,"Missing Location Information"));
			}
			if(!higherAdminRecords.isEmpty()) {
				invalidRecords.add(new InvalidRecords(higherAdminRecords,"Insufficient location information at "+ adminCodeToCommonName(commonType) +" level"));
			}
			JobRecords jobRecords = new JobRecords(recordsToCheck, invalidRecords, locations.size());
			return jobRecords;
			}
			catch (PipelineException pe) {
				throw pe;
			}
		catch (Exception e) {
			throw new DisjoinerException("Uncaught Disjoiner error:"+e.getMessage(), "Error Disjoining Locations");
		}
	}

	/**
	 * Checks if the suspected suspectedAncestor is actually an ancestor to the given Geoname location
	 * @param suspectedAncestor
	 * @param location
	 * @return true if suspectedAncestor is an ancestor of location, false otherwise
	 * @throws DisjoinerException 
	 */
	private boolean isAncestor(Location suspectedAncestor, Location location) throws DisjoinerException {
	    Set<Long> locationAncestors = ancestors.get(location.getAccession());
	    if (locationAncestors == null) {
	    	throw new DisjoinerException("Null Ancestors for location ID:\t"+location.getGeonameID(), "Error Disjoining Locations");
	    }
	    else if (location.getGeonameID().equals(suspectedAncestor.getGeonameID()) || locationAncestors.contains(suspectedAncestor.getGeonameID())) {
			return true;
		}
	    else {
	    	return false;
	    }
	}

	/**
	 * Update a record's Geoname Location
	 * @param record
	 * @param locationName
	 */
	private void setLocationName(GenBankRecord record, String locationName) {
		Location recordLocation = record.getGeonameLocation();
		recordLocation.setLocation(locationName);
		record.setGeonameLocation(recordLocation);
	}
	
	/**
	 * @return Map of US States and their Geoname IDs
	 */
	private static Map<Long, String> setupStateMap() {
		Map<Long, String> usStates = new LinkedHashMap<Long, String>(55, 1.0f);
		usStates.put(4829764L, "alabama");
		usStates.put(5879092L, "alaska");
		usStates.put(5551752L, "arizona");
		usStates.put(4099753L, "arkansas");
		usStates.put(5332921L, "california");
		usStates.put(5417618L, "colorado");
		usStates.put(4831725L, "connecticut");
		usStates.put(4142224L, "delaware");
		usStates.put(4155751L, "florida");
		usStates.put(4197000L, "georgia");
		usStates.put(5855797L, "hawaii");
		usStates.put(5596512L, "idaho");
		usStates.put(4896861L, "illinois");
		usStates.put(4921868L, "indiana");
		usStates.put(4862182L, "iowa");
		usStates.put(4273857L, "kansas");
		usStates.put(6254925L, "kentucky");
		usStates.put(4331987L, "louisiana");
		usStates.put(4971068L, "maine");
		usStates.put(4361885L, "maryland");
		usStates.put(6254926L, "massachusetts");
		usStates.put(5001836L, "michigan");
		usStates.put(5037779L, "minnesota");
		usStates.put(4436296L, "mississippi");
		usStates.put(4398678L, "missouri");
		usStates.put(5667009L, "montana");
		usStates.put(5073708L, "nebraska");
		usStates.put(5509151L, "nevada");
		usStates.put(5090174L, "new hampshire");
		usStates.put(5101760L, "new jersey");
		usStates.put(5481136L, "new mexico");
		usStates.put(5128638L, "new york");
		usStates.put(4482348L, "north carolina");
		usStates.put(5690763L, "north dakota");
		usStates.put(5165418L, "ohio");
		usStates.put(4544379L, "oklahoma");
		usStates.put(5744337L, "oregon");
		usStates.put(6254927L, "pennsylvania");
		usStates.put(5224323L, "rhode island");
		usStates.put(4597040L, "south carolina");
		usStates.put(5769223L, "south dakota");
		usStates.put(4662168L, "tennessee");
		usStates.put(4736286L, "texas");
		usStates.put(5549030L, "utah");
		usStates.put(5242283L, "vermont");
		usStates.put(6254928L, "virginia");
		usStates.put(5815135L, "washington");
		usStates.put(4826850L, "west virginia");
		usStates.put(5279468L, "wisconsin");
		usStates.put(5843591L, "wyoming");
		usStates.put(4138106L, "district of columbia");
		return usStates;
	}
	
	/**
	 * Specific Disjoning for Default GLM use case
	 * @param records
	 * @return Records with locations normalized to US States
	 * @throws PipelineException 
	 * @throws LuceneSearcherException 
	 */
	public JobRecords disjoinRecordsToStates(List<GenBankRecord> recordsToCheck) throws PipelineException {
		List<ExcludedRecords> missingLocationRecords = new LinkedList<>();
		List<ExcludedRecords> outsideUSRecords = new LinkedList<>();
		List<ExcludedRecords> higherAdminRecords = new LinkedList<>();
		List<ExcludedRecords> unknownExclusion = new LinkedList<>();
		try {
			US_STATES = setupStateMap();
			recordIter = recordsToCheck.listIterator();
			while (recordIter.hasNext()) {
				GenBankRecord record = recordIter.next();
				if (record.getGeonameLocation() == null || Normalizer.normalizeLocation(record.getGeonameLocation()).equalsIgnoreCase("unknown") || record.getGeonameLocation().getGeonameType() == null ) {
					recordIter.remove();
					missingLocationRecords.add(new ExcludedRecords(record.getAccession(), null));
				}
				else if (hierarchy.isParent("ADM1", record.getGeonameLocation().getGeonameType())) {
					recordIter.remove();
					higherAdminRecords.add(new ExcludedRecords(record.getAccession(), adminCodeToCommonName(record.getGeonameLocation().getGeonameType())));
				}
				else {
					Set<Long> recordAncestors;
					try {
						recordAncestors = hierarchyIndexSearcher.findLocationAncestors(record.getGeonameLocation().getGeonameID().toString());
						if (recordAncestors == null) {
							recordIter.remove();
							higherAdminRecords.add(new ExcludedRecords(record.getAccession(), adminCodeToCommonName(record.getGeonameLocation().getGeonameType())));
						}
						else {
							recordAncestors.remove(record.getGeonameLocation().getGeonameID());
							if (recordAncestors.isEmpty()) {
								recordIter.remove();
								higherAdminRecords.add(new ExcludedRecords(record.getAccession(), adminCodeToCommonName(record.getGeonameLocation().getGeonameType())));
							}
						}
					}
					catch (LuceneSearcherException lse) {
						throw new DisjoinerException("Error retrieving location ancestors: "+lse.getMessage(), "Error Disjoining Locations");
					}
				}
			}
			recordIter = null;
		}
		catch (PipelineException pe) {
			throw pe;
		}
		catch (Exception e) {
			throw new DisjoinerException("Error initially screening record locations:\t"+e.getMessage(), "Error Filtering Locations");
		}
		//Is any of the records location is a us state. Is not if any of its ancestor a us state. If NO remove else add into state set to count it.
		Set<String> states = new LinkedHashSet<String>(50);
		recordIter = recordsToCheck.listIterator();
		while (recordIter.hasNext()) {
			GenBankRecord record = recordIter.next();
			try {
				Location recLocation = record.getGeonameLocation();
				Long stateID = recLocation.getGeonameID();
				String stateLocation = US_STATES.get(stateID);
				if (stateLocation == null) {
					Set<Long> ancestors = hierarchyIndexSearcher.findLocationAncestors(record.getGeonameLocation().getGeonameID().toString());
					Iterator<Long> iter = ancestors.iterator();
					while (iter.hasNext() && stateLocation == null) {
						stateID = iter.next();
						stateLocation = US_STATES.get(stateID);
					}
				}
				if (stateLocation == null) {
					// could not map to state
					recordIter.remove();
					outsideUSRecords.add(new ExcludedRecords(record.getAccession(),null));
				}
				else {
					recLocation.setGeonameID(stateID);
					recLocation.setGeonameType("ADM1");
					recLocation.setLocation(stateLocation);
					record.setGeonameLocation(recLocation);
					states.add(stateLocation);
				}
			}
			catch (Exception e) {
				// issue record
				recordIter.remove();
				unknownExclusion.add(new ExcludedRecords(record.getAccession(),null));
			}
		}
		
		recordIter = null;
		if (states.size() < 2) {
			String userErr = "Too few distinct locations (need at least 2): " + states.size();
			if (states.size() == 1) {
				userErr += "\nLocation: "+states.iterator().next();
			}
			throw new DisjoinerException("Too few distinct locations: "+states.size(),userErr);
		}
		else if (states.size() > MAX_STATES) {
			StringBuilder userErr = new StringBuilder("Too many distinct locations (limit is "+MAX_STATES+"): " + states.size());
			userErr.append("\nLocations: ");
			for (String state : states) {
				userErr.append("\n\t");
				userErr.append(state);
			}
			throw new DisjoinerException("Too many distinct locations: "+states.size(), userErr.toString());
		}
		else {
			List<InvalidRecords> invalidRecords = new LinkedList<>();
			if(!missingLocationRecords.isEmpty()) {
				invalidRecords.add(new InvalidRecords(missingLocationRecords,"Missing Location Information"));
			}
			if(!higherAdminRecords.isEmpty()) {
				invalidRecords.add(new InvalidRecords(higherAdminRecords,"Insufficient location information at State level"));
			}
			if(!outsideUSRecords.isEmpty()) {
				invalidRecords.add(new InvalidRecords(outsideUSRecords,"Unable to map location to a state in US"));
			}
			if(!unknownExclusion.isEmpty()) {
				invalidRecords.add(new InvalidRecords(unknownExclusion,"Unknow Error"));
			}
			JobRecords validRecords = new JobRecords(recordsToCheck, invalidRecords, states.size());
			return validRecords;
		}
	}
	
	private static Map<String, String> setupAdminLevelMap() {
		Map<String, String> adminLevels = new LinkedHashMap<String, String>();
		adminLevels.put("PCLI", "Country");
		adminLevels.put("ADM1", "Province/State");
		adminLevels.put("ADM2", "County/City");
		return adminLevels;
	}
	
	private String adminCodeToCommonName(String adminCode) {
		String commonName = adminLevel.get(adminCode);
		if(commonName == null) {
			return adminCode;
		}
		return commonName;
	}
}
