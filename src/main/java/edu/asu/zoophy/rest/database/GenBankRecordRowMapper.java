package edu.asu.zoophy.rest.database;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import edu.asu.zoophy.rest.genbank.GenBankRecord;
import edu.asu.zoophy.rest.genbank.Host;
import edu.asu.zoophy.rest.genbank.Location;
import edu.asu.zoophy.rest.genbank.Sequence;

/**
 * Maps SQL row to core GenBankRecord
 * @author devdemetri
 */
public class GenBankRecordRowMapper implements RowMapper<GenBankRecord> {

	@Override
	public GenBankRecord mapRow(ResultSet row, int rowNumber) throws SQLException {
		GenBankRecord record = new GenBankRecord();
		final String recordAccession = row.getString("Accession");
		record.setAccession(recordAccession);
		Sequence sequence = new Sequence();
		sequence.setAccession(recordAccession);
		sequence.setDate(row.getString("Collection_Date"));
		sequence.setCollectionDate(row.getString("Normalized_Date"));
		sequence.setComment(row.getString("Comment"));
		sequence.setDefinition(row.getString("Definition"));
		sequence.setIsolate(row.getString("Isolate"));
		sequence.setOrganism(row.getString("Organism"));
		sequence.setSegmentLength(row.getInt("Segment_Length"));
		sequence.setRawSequence(row.getString("Sequence"));
		sequence.setStrain(row.getString("Strain"));
		sequence.setTaxID(row.getInt("Tax_ID"));
		sequence.setPH1N1(row.getBoolean("pH1N1"));
		record.setSequence(sequence);
		Location location = new Location();
		location.setAccession(recordAccession);
		location.setGeonameID(row.getLong("Geoname_ID"));
		location.setLocation(row.getString("Location"));
		location.setGeonameType(row.getString("Type"));
		location.setLatitude(row.getDouble("Latitude"));
		location.setLongitude(row.getDouble("Longitude"));
		location.setState(row.getString("State"));
		location.setCountry(row.getString("Country"));
		record.setGeonameLocation(location);
		Host host = new Host();
		host.setAccession(recordAccession);
		host.setName(row.getString("Host_Name"));
		host.setTaxon(row.getInt("Host_taxon"));
		record.setHost(host);
		return record;
	}

}
