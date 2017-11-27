package edu.asu.zoophy.rest.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.springframework.jdbc.core.ResultSetExtractor;

import edu.asu.zoophy.rest.genbank.PossibleLocation;

/**
 * Maps SQL row to List of Locations
 * @author amagge
 */
public class LocationsRSExtractor implements ResultSetExtractor<List<PossibleLocation>> {
	public static final Logger logger = org.slf4j.LoggerFactory.getLogger(LocationsRSExtractor.class);
	
	@Override
	public List<PossibleLocation> extractData(ResultSet rs) throws SQLException {
		List<PossibleLocation> locations = new ArrayList<PossibleLocation>();
		while(rs.next()){
			PossibleLocation loc = new PossibleLocation();
			loc.setGeonameID(rs.getLong("Geoname_ID"));
			loc.setLocation(rs.getString("Location"));
			loc.setLatitude(rs.getDouble("Latitude"));
			loc.setLongitude(rs.getDouble("Longitude"));
			loc.setProbability(rs.getDouble("probability"));
			locations.add(loc);
		}
		logger.info("Returning " + locations.size() + " locations.");
		return locations;
	}

}
