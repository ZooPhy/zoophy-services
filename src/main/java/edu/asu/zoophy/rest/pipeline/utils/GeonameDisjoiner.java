package edu.asu.zoophy.rest.pipeline.utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
	private final int MAX_DISTINCT_LOCATIONS;
	private Map<Long, String> US_STATES;
	private Map<String, String> adminLevel;
	private final Logger log;
	private final int DISJOIN_THRESHOLD;
	
	private final String MISSING_LOCATION = "Missing Location information";
	private final String INCOMPLETE_HIERARCHY = "Incomplete Location Hierarchy inforamtion";
	private final String HIGHER_ADMIN_LEVEL = "Insufficient location information at ";
	private final String DEFAULT_DISJOIN_LEVEL = "PCLI";

	public GeonameDisjoiner(LuceneHierarchySearcher hierarchyIndexSearcher) throws PipelineException {
		this.hierarchyIndexSearcher = hierarchyIndexSearcher;
		PropertyProvider provider = PropertyProvider.getInstance();
		adminLevel = setupAdminLevelMap();
		MAX_DISTINCT_LOCATIONS = Integer.parseInt(provider.getProperty("job.max.locations"));
		DISJOIN_THRESHOLD = Integer.parseInt(provider.getProperty("job.disjoiner.threshold"));
		log = Logger.getLogger("GeonameDisjoiner");
	}
	
	/**
	 * Disjoins given list of records, filtering out records with invalid or too general locations. Remaining locations are normalized to a set of disjoint locations.
	 * @param recordsToCheck - list of records to Disjoin
	 * @return Filtered records with valid, disjoint Geoname locations.
	 * @throws DisjoinerException
	 * @throws GLMException 
	 * @throws GeoHierarchyException 
	 */
	public JobRecords disjoinRecords(List<GenBankRecord> recordsToCheck) throws DisjoinerException, GLMException, GeoHierarchyException { 
		List<GenBankRecord> validRecords = new LinkedList<>();
		Map<String,List<GenBankRecord>> countryBasedRecords = new HashMap<>();
		Set<Location> distinctLocations = new LinkedHashSet<Location>(50);
		List<InvalidRecords> invalidRecords = new LinkedList<>();
		Map<String,Set<Long>> ancestors = null;
		
		DisjoinerCleanUpResponse filteredRecords = removeIncompleteRecords(recordsToCheck);
		ancestors = filteredRecords.getAncestors();
		recordsToCheck = filteredRecords.getJobRecords().getValidRecordList();
		invalidRecords.addAll(filteredRecords.getJobRecords().getInvalidRecordList());
		
		countryBasedRecords =  generateCountryMap(recordsToCheck);
		distinctLocations = setupInitialDistinctLocations(countryBasedRecords);
		if(countryBasedRecords.size() > MAX_DISTINCT_LOCATIONS) {
			errorTooManyLocations(distinctLocations);
		}
		for(Map.Entry<String, List<GenBankRecord>> entry: countryBasedRecords.entrySet()) {
			String country = entry.getKey();
			List<GenBankRecord> records = entry.getValue();
			Boolean oneCountry = false;		//is job has just one country
			distinctLocations = removeFromDistinctLocations(distinctLocations, country);
			
			Map<String,Integer> types = adminLevelsMap(records);
			String disjoinLevel = calculateDisjoinLevel(types,countryBasedRecords.size());
			log.info("Admin levels in "+country +": "+types);
			log.info("Disjoiner level: "+disjoinLevel);
			if(distinctLocations.size() == 0) {
				oneCountry = true;
			}
			DisjoinerResponse filteredCountryRecords = disjoinRecords(records, disjoinLevel, distinctLocations, ancestors, false, oneCountry);
			
			validRecords.addAll(filteredCountryRecords.getValidRecordList());
			invalidRecords.addAll(filteredCountryRecords.getInvalidRecordList());
			distinctLocations = filteredCountryRecords.getDistinctLocations();
		}
		
		log.info("Distinct locations: "+distinctLocations.size());
		if (distinctLocations.size() < 2) {
			errorTooFewLocations(distinctLocations);	
		}
		JobRecords jobRecords = new JobRecords(validRecords, invalidRecords, distinctLocations.size());
		return jobRecords;
	}
	
	/**
	 * Remove any record which is missing location information or information about it;s ancestors
	 * @param recordsToCheck - list of records to DisJoin
	 * @return DisjoinerCleanUpResponse
	 * @throws DisjoinerException 
	 */
	private DisjoinerCleanUpResponse removeIncompleteRecords(List<GenBankRecord> recordsToCheck) throws DisjoinerException {
		List<ExcludedRecords> missingLocationRecords = new LinkedList<>();
		List<ExcludedRecords> incompleteHierarchy = new LinkedList<>();
		List<InvalidRecords> invalidRecords = new LinkedList<>();
		Iterator<GenBankRecord> Iter = recordsToCheck.listIterator();
		Map<String,Set<Long>> ancestors = new HashMap<String,Set<Long>>((int)(recordsToCheck.size())+1, 1.0f);
		try {
			while (Iter.hasNext()) {
				GenBankRecord record = Iter.next();
				if (record.getGeonameLocation() == null || record.getGeonameLocation().getCountry() == null ||
						Normalizer.normalizeLocation(record.getGeonameLocation()).equalsIgnoreCase("unknown") || record.getGeonameLocation().getGeonameType() == null) {
					missingLocationRecords.add(new ExcludedRecords(record.getAccession(), null));
					Iter.remove();
				}
				else {
					Set<Long> recordAncestors;
					try {
						recordAncestors = hierarchyIndexSearcher.findLocationAncestors(record.getGeonameLocation().getGeonameID().toString());
						if (recordAncestors == null) {
							incompleteHierarchy.add(new ExcludedRecords(record.getAccession(), null));
							Iter.remove();
						}
						else {
							recordAncestors.remove(record.getGeonameLocation().getGeonameID());
							if (recordAncestors.isEmpty()) {
								incompleteHierarchy.add(new ExcludedRecords(record.getAccession(), null));
								Iter.remove();
							} else {
								ancestors.put(record.getAccession(), recordAncestors);
							}
						}
					}
					catch (LuceneSearcherException lse) {
						throw new DisjoinerException("Error retrieving location ancestors: "+lse.getMessage(), "Error Disjoining Locations");
					}
				}
			}
			Iter = null;
			if(!missingLocationRecords.isEmpty()) {
				invalidRecords.add(new InvalidRecords(missingLocationRecords,MISSING_LOCATION));
			}
			if(!incompleteHierarchy.isEmpty()) {
				invalidRecords.add(new InvalidRecords(incompleteHierarchy,INCOMPLETE_HIERARCHY));
			}
			return new DisjoinerCleanUpResponse(new JobRecords(recordsToCheck, invalidRecords, null), ancestors);
		}
		catch (PipelineException pe) {
			throw pe;
		}
		catch (Exception e) {
			throw new DisjoinerException("Error initially screening record locations:\t"+e.getMessage(), "Error Filtering Locations");
		}
	}
	
	/**
	 * Normalized locations to a set of disjoint locations based on the administration level.
	 * @param recordsToCheck - list of records to Disjoin
	 * @param commonType - administration level 
	 * @param distinctLocations - set of distinct locations
	 * @param ancestors - map of locations and their ancestors
	 * @param rerun - disjoiner is rerun on different commonLevel
	 * @param oneCountry - job has just one country
	 * @return Filtered records with valid, disjoint Geoname locations.
	 * @throws DisjoinerException
	 * @throws GLMException 
	 * @throws GeoHierarchyException 
	 */
	public DisjoinerResponse disjoinRecords(List<GenBankRecord> records, String commonLevel, Set<Location> distinctLocations, Map<String,Set<Long>> ancestors, Boolean rerun, Boolean oneCountry) throws DisjoinerException, GLMException, GeoHierarchyException {
		List<GenBankRecord> allRecords = new LinkedList<>();
		List<ExcludedRecords> higherAdminRecords = new LinkedList<>();
		List<InvalidRecords> invalidRecords = new LinkedList<>();
		Iterator<GenBankRecord> recordIter = null;
		Set<Location> locations = new LinkedHashSet<Location>(50);
		Set<Location> locationsToRemove;
		Map<Long,String> idToLocation = new HashMap<Long,String>(50);
		try {
			try {
				recordIter = records.listIterator();
				while (recordIter.hasNext()) {
					GenBankRecord record = recordIter.next();
					allRecords.add((GenBankRecord) record.clone());
					Location recordLocation = record.getGeonameLocation();
					boolean isDisjoint = true;
					if (hierarchy.isParent(commonLevel, recordLocation.getGeonameType())) {
						//remove any record with admin level above the selected admin level 
						isDisjoint = false;
						recordIter.remove();
						higherAdminRecords.add(new ExcludedRecords(record.getAccession(), adminCodeToCommonName(record.getGeonameLocation().getGeonameType())));
					}
					else {
						/*
						 * Fill up the locations set and do step1 of pre-pruning :
						 * 1. Ignore same locations
						 * 2. Ignore locations which have ancestors along the set of records and update those locations with 
						 * 	  their	ancestors to reduce load at the time of pruning
						 * e.g. - do not include Arizona if we already have Tempe, AZ
						 */
						for (Location parent : locations) {
							if (isAncestor(ancestors, parent, recordLocation)) {
								isDisjoint = false;
								if (!parent.getGeonameID().equals(recordLocation.getGeonameID())) {
									record.setGeonameLocation(parent);
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
			/*
			 * Step 2 of pre-pruning:
			 * All the ancestors were not removed in step1
			 * Remove from location set and update record locations which have ancestors
			 */
			try {
				recordIter = records.listIterator();
				while (recordIter.hasNext()) {
					GenBankRecord record = recordIter.next();
					Location location = record.getGeonameLocation();
					for (Location locationParent : locations) {
						if (!(locationParent.getGeonameID().equals(location.getGeonameID()) || locationsToRemove.contains(locationParent))) {
							if (isAncestor(ancestors, locationParent, location)) {
								if (!location.getGeonameID().equals(locationParent.getGeonameID())) {
									record.setGeonameLocation(locationParent);
								}
								locationsToRemove.add(location);
								break;
							}
						}
					}	
				}
				locations.removeAll(locationsToRemove);
			}catch (PipelineException pe) {
				throw pe;
			}
			catch (Exception e) {
				throw new DisjoinerException("Error removing overlapping locations:\t"+e.getMessage(), "Error Disjoining Locations");
			}
			locationsToRemove.clear();
			/*
			 * Pruning - Prune the locations by updating the location of each record by replacing it with location at commonLevel
			 */
			Map<Long, Location> selectedAncestor = new HashMap<>();
			recordIter = records.listIterator();
			while (recordIter.hasNext()) {
				GenBankRecord record = recordIter.next();
				Location location = record.getGeonameLocation();
				if(!location.getGeonameType().equals(commonLevel)) {
					Location courselocation = selectedAncestor.get(location.getGeonameID());
					if(courselocation==null) {
						Set<Long> ancestorList = ancestors.get(record.getAccession());
						courselocation = fineToCoarseLocation(ancestorList, commonLevel);
						selectedAncestor.put(location.getGeonameID(), courselocation);
					}
					if(courselocation!=null) {
						locationsToRemove.add(location);
						record.setGeonameLocation(courselocation);
						locations = addToLocations(locations, courselocation);
					}else {
						log.info("Couldn't find coarse location for " + record.getAccession());
					}
				}
			}
			locations.removeAll(locationsToRemove);
			recordIter = null;
		
			idToLocation.clear();
			distinctLocations.addAll(locations);	
			if(!higherAdminRecords.isEmpty()) {
				invalidRecords.add(new InvalidRecords(higherAdminRecords,HIGHER_ADMIN_LEVEL+ adminCodeToCommonName(commonLevel) +" level"));
			}
			DisjoinerResponse disjoinerResponse = new DisjoinerResponse(records, invalidRecords, distinctLocations, false);
			if(distinctLocations.size() > MAX_DISTINCT_LOCATIONS) {
				if(!commonLevel.equals(DEFAULT_DISJOIN_LEVEL)) {
					distinctLocations.removeAll(locations);
					disjoinerResponse = disjoinRecords(allRecords, DEFAULT_DISJOIN_LEVEL, distinctLocations, ancestors, true, oneCountry);
				}else {
					errorTooManyLocations(distinctLocations);
				}
			}
			//Handle the case where the job has too many (>MAX_DISTINCT_LOCATIONS) records and all of them are from the same country
			//the disjoiner will first have TooManyLocations and in the re-run with DEFAULT_DISJOIN_LEVEL, it will have TooFewLocations
			if(disjoinerResponse.getTooManyLocations()) {
				errorTooManyLocations(locations);
			}
			if(distinctLocations.size() < 2 && rerun && oneCountry) {
				disjoinerResponse.setTooManyLocations(true);
			}
			return disjoinerResponse;
		}
		catch (PipelineException pe) {
			throw pe;
		}catch (Exception e) {
			throw new DisjoinerException("Uncaught Disjoiner error:"+e.getMessage(), "Error Disjoining Locations");
		}
	}
	

	/*
	 * For a given location get the ancestor with level equal to the commonLevel
	 */
	private Location fineToCoarseLocation(Set<Long> ancestors, String commonLevel) {
		Location location = null;
		 for(Long ancestor: ancestors) {
			 try {
				location = hierarchyIndexSearcher.findGeonameLocation(String.valueOf(ancestor));
				if(location!=null) {
					String level = location.getGeonameType();
					if(level!=null && level.equalsIgnoreCase(commonLevel)) {
						break;
					}
				}
			} catch (LuceneSearcherException e) {
				location = null;
			}
		 }
		 return location;
	}
	
	private Set<Location> addToLocations(Set<Location> locations, Location location){
		boolean exists = false;
		for(Location loc : locations) {
			if(String.valueOf(loc.getGeonameID()).equals(String.valueOf(location.getGeonameID()))) {
				exists = true;
				break;
			}
		}
		if(!exists) {
			locations.add(location);
		}
		return locations;
	}

	private void errorTooFewLocations(Set<Location> distinctLocations) throws DisjoinerException{
		String userErr = "Too few distinct locations (need at least 2): " + distinctLocations.size();
		if (distinctLocations.size() == 1) {
			userErr += "\nLocation: "+distinctLocations.iterator().next().getLocation();
		}
		throw new DisjoinerException("Too few distinct locations: "+distinctLocations.size(),userErr);
	}
	
	private void errorTooManyLocations(Set<Location> distinctLocations) throws DisjoinerException {
		StringBuilder userErr = new StringBuilder("Too many distinct locations (limit is "+MAX_DISTINCT_LOCATIONS+"): " + distinctLocations.size());
		userErr.append("\nLocations: ");
		for (Location location : distinctLocations) {
			userErr.append(location.getLocation());
			userErr.append(", ");
		}
		throw new DisjoinerException("Too many distinct locations: "+distinctLocations.size(), userErr.substring(0,userErr.length()-2));
	}
	
	/**
	 * Initially distinctLocation set is filled with distinct countries of records
	 * @param countryBasedRecords - map of countries and it's corresponding records
	 * @return Set of distinct locations.
	 */
	private Set<Location> setupInitialDistinctLocations(Map<String,List<GenBankRecord>> countryBasedRecords) {
		Set<Location> distinctLocations = new LinkedHashSet<Location>(50);
		for(String country: countryBasedRecords.keySet()) {
			Location location = new Location();
			location.setLocation(country);
			distinctLocations.add(location);
		}
		return distinctLocations;
	}
	
	/**
	 * Before running the disJoiner for a country remove that country from the list of distinct locations
	 * since it will be replaced by finer distinct locations after the disJoiner is run.
	 * @param countryBasedRecords - map of countries and it's corresponding records
	 * @param country - name of the country to be removed from the set
	 * @return Set of distinct locations.
	 */
	private Set<Location> removeFromDistinctLocations(Set<Location> distinctLocations, String country) {
		Iterator<Location> iter = distinctLocations.iterator();
		while (iter.hasNext()) {
			Location location = iter.next();
			if(location.getLocation()!=null &&location.getLocation().equals(country)&&location.getGeonameID()==null) {
				iter.remove();
			}
		}
		return distinctLocations;
	}
	
	private Map<String,List<GenBankRecord>> generateCountryMap(List<GenBankRecord> recordsToCheck) {
		Iterator<GenBankRecord> iter = recordsToCheck.listIterator();
		Map<String,List<GenBankRecord>> countries = new LinkedHashMap<String,List<GenBankRecord>>();

		while (iter.hasNext()) {
			GenBankRecord record = iter.next();
			String country = record.getGeonameLocation().getCountry().trim();								
			if (countries.get(country) == null) {
				List<GenBankRecord> records = new LinkedList<GenBankRecord>();
				countries.put(country, records);
			}
			List<GenBankRecord> records = countries.get(country);
			records.add(record);
			countries.put(country, records);
		}
		
		countries = sortCountryMap(countries);
		return countries;
	}
	
	/**
	 * Creates a map of administrative level and number of locations belonging to that level
	 * @param genBankRecords - lost of genBank records
	 * @return Map of admin level and count
	 */
	private Map<String,Integer> adminLevelsMap(List<GenBankRecord> genBankRecords) {
		Map<String,Integer> types = new LinkedHashMap<>();
		Iterator<GenBankRecord> iter = genBankRecords.listIterator();
		while (iter.hasNext()) {
			GenBankRecord record = iter.next();
				String type = record.getGeonameLocation().getGeonameType();	
				if (types.get(type) == null) {
					types.put(type, 0);
				}
				int count = types.get(type);
				types.put(type, ++count);
		}
		return types;
	}
	
	/**
	 * Select administrative level for a country based on a threshold
	 * @param types - map of admin level and count for a country
	 * @param countryCount - number of countries for this job
	 * @return administrative level
	 */
	private String calculateDisjoinLevel(Map<String,Integer> types, int countryCount) throws GeoHierarchyException {
		if(countryCount==1) {
			return "ADM1";
		}
		Map<String, Integer> levelCount = new HashMap<>();
		String level = DEFAULT_DISJOIN_LEVEL;
		int totalCount = 0;
		for (String type : types.keySet()) {
			int count = types.get(type); 
			totalCount += count;
			for (String subType : types.keySet()) {
				if(!subType.equals(type) && hierarchy.isParent(subType, type)) {
					count += types.get(subType);
				}
			}
			levelCount.put(type, count);
		}
		levelCount = sortByValue(levelCount);
		
		String prevLevel = DEFAULT_DISJOIN_LEVEL;
		for(String type : levelCount.keySet()) {
			int count  = levelCount.get(type);
			float exclusionPercent = 100 - ((float) count/totalCount)*100;
			//Select this level only if less than 50%(DISJOIN_THRESHOLD) of the records are excluded
			if(exclusionPercent > DISJOIN_THRESHOLD) {
				level = prevLevel;
				break;
			}
			level = type;
			prevLevel = type;
		}
		return level;
	}
	
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			@Override
			public int compare(Map.Entry<K, V> e1, Map.Entry<K, V> e2) {
				return - (e1.getValue()).compareTo(e2.getValue());
			}
		});
	 
		Map<K, V> result = new LinkedHashMap<>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}
	
	/*
	 * Sort records-country map so that country with most records is disJoined first
	 */
	public static Map<String, List<GenBankRecord>> sortCountryMap(final Map<String, List<GenBankRecord>> orig){
	    final Comparator<String> c = new Comparator<String>(){
	        @Override
	        public int compare(final String o1, final String o2){
	        		final int sizeCompare = -( orig.get(o1).size() - orig.get(o2).size());
	            return sizeCompare != 0 ? sizeCompare : o1.compareTo(o2);
	        }
	    };
	    final Map<String, List<GenBankRecord>> ret = new TreeMap<String, List<GenBankRecord>>(c);
	    ret.putAll(orig);
	    return ret;
	}
	
	/**
	 * Checks if the suspected suspectedAncestor is actually an ancestor to the given Geoname location
	 * @param map of location and it's ancestors
	 * @param suspectedAncestor
	 * @param location
	 * @return true if suspectedAncestor is an ancestor of location, false otherwise
	 * @throws DisjoinerException 
	 */
	private boolean isAncestor(Map<String,Set<Long>> ancestors, Location suspectedAncestor, Location location) throws DisjoinerException {
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
		Iterator<GenBankRecord> recordIter = null;
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
		else if (states.size() > MAX_DISTINCT_LOCATIONS) {
			StringBuilder userErr = new StringBuilder("Too many distinct locations (limit is "+MAX_DISTINCT_LOCATIONS+"): " + states.size());
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
