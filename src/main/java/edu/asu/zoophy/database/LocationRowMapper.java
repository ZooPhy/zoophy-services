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
	public Location mapRow(ResultSet row, int rowNumber) throws SQLException {
		Location location = new Location();
		location.setAccession(row.getString("Accession"));
		location.setGeonameID(row.getLong("Geoname_ID"));
		location.setLocation(row.getString("Location"));
		location.setGeonameType(row.getString("Type"));
		location.setLatitude(row.getDouble("Latitude"));
		location.setLongitude(row.getDouble("Longitude"));
		location.setCountry(row.getString("Country"));
		return location;
	}

}
