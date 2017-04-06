package edu.asu.zoophy.rest.pipeline.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.asu.zoophy.rest.genbank.GenBankRecord;
import edu.asu.zoophy.rest.genbank.Location;
import edu.asu.zoophy.rest.index.LuceneSearcher;
import edu.asu.zoophy.rest.index.LuceneSearcherException;
import edu.asu.zoophy.rest.pipeline.PipelineException;
import edu.asu.zoophy.rest.pipeline.PropertyProvider;
import edu.asu.zoophy.rest.pipeline.glm.GLMException;


/**
 * Responsible for making sure ZooPhy job locations are disjoint
 * @author devdemetri
 */
public class GeonameDisjoiner {

	private final LuceneSearcher indexSearcher;
	private static final GeoHierarchy hierarchy = GeoHierarchy.getInstance();
	private final int MAX_STATES;
	private Map<String,Set<Long>> ancestors;
	private final Map<String, Long> US_STATES;
	private final long BAD_DISJOIN = -1L;
	private Iterator<GenBankRecord> recordIter = null;
	
	public GeonameDisjoiner(LuceneSearcher indexSearcher) throws PipelineException {
		this.indexSearcher = indexSearcher;
		PropertyProvider provider = PropertyProvider.getInstance();
		MAX_STATES = Integer.parseInt(provider.getProperty("job.max.locations"));
		US_STATES = setupStateMap();
	}
	
	/**
	 * Disjoins given list of records, filtering out records with invalid or too general locations. Remaining locations are normalized to a set of disjoint locations.
	 * @param recordsToCheck - list of records to Disjoin
	 * @return Filtered records with valid, disjoint Geoname locations.=
	 * @throws DisjoinerException
	 * @throws GLMException 
	 * @throws GeoHierarchyException 
	 */
	public List<GenBankRecord> disjoinRecords(List<GenBankRecord> recordsToCheck, boolean usingDefaultGLM) throws DisjoinerException, GLMException, GeoHierarchyException {
		try {
			Map<Long,Long> disjoins = new HashMap<Long,Long>((int)(recordsToCheck.size()*.75)+1);
			Set<Location> locations = new LinkedHashSet<Location>(50);
			Map<String,Integer> types = new LinkedHashMap<String,Integer>();
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
						recordIter.remove();
					}
					else {
						Set<Long> recordAncestors;
						try {
							recordAncestors = indexSearcher.findLocationAncestors(record.getAccession());
							if (recordAncestors == null) {
								recordIter.remove();
							}
							else {
								recordAncestors.remove(record.getGeonameLocation().getGeonameID());
								if (recordAncestors.isEmpty()) {
									recordIter.remove();
								}
								else {
									String type = record.getGeonameLocation().getGeonameType();
									if (types.get(type) == null) {
										types.put(type, 0);
									}
									types.put(type, (types.get(type)+1));
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
			if (usingDefaultGLM) {
				commonType = "ADM1";
			}
			else {
				for (String type : types.keySet()) {
					if (types.get(type) > maxType) {
						maxType = types.get(type);
						commonType = type;
					}
				}
			}
			try {
				recordIter = recordsToCheck.listIterator();
				while (recordIter.hasNext()) {
					GenBankRecord record = recordIter.next();
					Location recordLocation = record.getGeonameLocation();
					boolean isDisjoint = true;
					if (hierarchy.isParent(commonType, record.getGeonameLocation().getGeonameType())) {
						isDisjoint = false;
						recordIter.remove();
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
					boolean removed = false;
					for (Location locationParent : locations) {
						if (!(locationParent.getGeonameID().equals(location.getGeonameID()) || locationsToRemove.contains(locationParent))) {
							if (isAncestor(locationParent,location)) {
								if (!location.getGeonameID().equals(locationParent.getGeonameID())) {
									disjoins.put(location.getGeonameID(), locationParent.getGeonameID());
								}
								locationsToRemove.add(location);
								removed = true;
								break;
							}
						}
					}
					if (usingDefaultGLM && !removed) {
						location.setLocation(location.getLocation().toLowerCase());
						try {
							Location stateLocation = convertToState(location);
							if (!location.getGeonameID().equals(stateLocation.getGeonameID())) {
								locationsToRemove.add(location);
								locations.add(stateLocation);
								disjoins.put(location.getGeonameID(), stateLocation.getGeonameID());
							}
						}
						catch (GLMException glme) {
							locationsToRemove.add(location);
							removed = true;
							disjoins.put(location.getGeonameID(), BAD_DISJOIN);
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
			return recordsToCheck;
			}
			catch (PipelineException pe) {
				throw pe;
			}
		catch (Exception e) {
			throw new DisjoinerException("Uncaught Disjoiner error:"+e.getMessage(), "Error Disjoining Locations");
		}
	}
	
	/**
	 * Ensures that locations are US States for GLM predictor usage
	 * @param recordLocation
	 * @return
	 * @throws GLMException 
	 */
	private Location convertToState(Location recordLocation) throws GLMException {
		try {
			if (US_STATES.keySet().contains(recordLocation.getLocation().toLowerCase())) {
				return recordLocation;
			}
			else {
				boolean found = false;
				for (String state : US_STATES.keySet()) {
					if (recordLocation.getLocation().toLowerCase().contains(state)) {
						found = true;
						recordLocation.setLocation(state);
						recordLocation.setGeonameID(US_STATES.get(state));
					}
				}
				if (!found) {
					throw new GLMException("Could not match Location to US State: "+recordLocation.getLocation(), "Error matching Locations to US States");
				}
				return recordLocation;
			}
		}
		catch (GLMException glme) {
			throw glme;
		}
		catch (Exception e) {
			throw new GLMException("Error matching Location to US State: "+recordLocation.getLocation()+" : "+e.getMessage(), "Error matching Locations to US States");
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
	private static Map<String, Long> setupStateMap() { 
		Map<String, Long> usStates = new LinkedHashMap<String, Long>(50);
		usStates.put("alabama", 4829764L);
		usStates.put("alaska", 5879092L);
		usStates.put("arizona", 5551752L);
		usStates.put("arkansas", 4099753L);
		usStates.put("california", 5332921L);
		usStates.put("colorado", 5417618L);
		usStates.put("connecticut", 4831725L);
		usStates.put("delaware", 4142224L);
		usStates.put("florida", 4155751L);
		usStates.put("georgia", 4197000L);
		usStates.put("hawaii", 5855797L);
		usStates.put("idaho", 5596512L);
		usStates.put("illinois", 4896861L);
		usStates.put("indiana", 4921868L);
		usStates.put("iowa", 4862182L);
		usStates.put("kansas", 4273857L);
		usStates.put("kentucky", 6254925L);
		usStates.put("louisiana", 4331987L);
		usStates.put("maine", 4971068L);
		usStates.put("maryland", 4361885L);
		usStates.put("massachusetts", 6254926L);
		usStates.put("michigan", 5001836L);
		usStates.put("minnesota", 5037779L);
		usStates.put("mississippi", 4436296L);
		usStates.put("missouri", 4398678L);
		usStates.put("montana", 5667009L);
		usStates.put("nebraska", 5073708L);
		usStates.put("nevada", 5509151L);
		usStates.put("new hampshire", 5090174L);
		usStates.put("new jersey", 5101760L);
		usStates.put("new mexico", 5481136L);
		usStates.put("new york", 5128638L);
		usStates.put("north carolina", 4482348L);
		usStates.put("north dakota", 5690763L);
		usStates.put("ohio", 5165418L);
		usStates.put("oklahoma", 4544379L);
		usStates.put("oregon", 5744337L);
		usStates.put("pennsylvania", 6254927L);
		usStates.put("rhode island", 5224323L);
		usStates.put("south carolina", 4597040L);
		usStates.put("south dakota", 5769223L);
		usStates.put("tennessee", 4662168L);
		usStates.put("texas", 4736286L);
		usStates.put("utah", 5549030L);
		usStates.put("vermont", 5242283L);
		usStates.put("virginia", 6254928L);
		usStates.put("washington", 5815135L);
		usStates.put("west virginia", 4826850L);
		usStates.put("wisconsin", 5279468L);
		usStates.put("wyoming", 5843591L);
		usStates.put("district of columbia", 4138106L);
		return usStates;
	}
	
}
