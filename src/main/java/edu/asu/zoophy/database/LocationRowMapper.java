package edu.asu.zoophy.database;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import edu.asu.zoophy.genbank.Location;

/**
 * Maps SQL row to Geoname Location
 * @author devdemetri
 */
public class LocationRowMapper implements RowMapper<Location> {

	@Override
	public Location mapRow(ResultSet rs, int rowNum) throws SQLException {
		Location loc = new Location();
		loc.setAccession(rs.getString("Accession"));
		loc.setGeonameID(rs.getLong("Geoname_ID"));
		loc.setLocation(rs.getString("Location"));
		loc.setGeonameType(rs.getString("Type"));
		loc.setLatitude(rs.getDouble("Latitude"));
		loc.setLongitude(rs.getDouble("Longitude"));
		return loc;
	}

}
