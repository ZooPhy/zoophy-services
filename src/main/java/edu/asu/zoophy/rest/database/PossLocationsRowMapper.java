package edu.asu.zoophy.rest.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import org.springframework.jdbc.core.RowMapper;

import edu.asu.zoophy.rest.genbank.PossibleLocation;

/**
 * Maps SQL row to List of Locations
 * @author amagge
 */
public class PossLocationsRowMapper implements RowMapper<PossibleLocation> {
	public static final Logger logger = Logger.getLogger("PossLocationsRowMapper");
	
	@Override
	public PossibleLocation mapRow(ResultSet row, int rowNumber) throws SQLException {
		PossibleLocation loc = new PossibleLocation();
		loc.setGeonameID(row.getLong("Geoname_ID"));
		loc.setLocation(row.getString("Location"));
		loc.setLatitude(row.getDouble("Latitude"));
		loc.setLongitude(row.getDouble("Longitude"));
		loc.setProbability(row.getDouble("probability"));
		return loc;
	}

}
