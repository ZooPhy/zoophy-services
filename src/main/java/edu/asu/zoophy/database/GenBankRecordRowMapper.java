package edu.asu.zoophy.database;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import edu.asu.zoophy.genbank.GenBankRecord;
import edu.asu.zoophy.genbank.Host;
import edu.asu.zoophy.genbank.Location;
import edu.asu.zoophy.genbank.Sequence;

/**
 * Maps SQL row to core GenBankRecord
 * @author devdemetri
 */
public class GenBankRecordRowMapper implements RowMapper<GenBankRecord> {

	@Override
	public GenBankRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
		GenBankRecord rec = new GenBankRecord();
		final String acc = rs.getString("Accession");
		rec.setAccession(acc);
		Sequence seq = new Sequence();
		seq.setAccession(acc);
		seq.setCollectionDate(rs.getString("Collection_Date"));
		seq.setComment(rs.getString("Comment"));
		seq.setDefinition(rs.getString("Definition"));
		seq.setIsolate(rs.getString("Isolate"));
		seq.setOrganism(rs.getString("Organism"));
		seq.setSegmentLength(rs.getInt("Segment_Length"));
		seq.setRawSequence(rs.getString("Sequence"));
		seq.setStrain(rs.getString("Strain"));
		seq.setTaxID(rs.getInt("Tax_ID"));
		rec.setSequence(seq);
		Location loc = new Location();
		loc.setAccession(acc);
		loc.setGeonameID(rs.getLong("Geoname_ID"));
		loc.setLocation(rs.getString("Location"));
		loc.setGeonameType(rs.getString("Type"));
		loc.setLatitude(rs.getDouble("Latitude"));
		loc.setLongitude(rs.getDouble("Longitude"));
		loc.setCountry(rs.getString("Country"));
		rec.setGeonameLocation(loc);
		Host host = new Host();
		host.setAccession(acc);
		host.setName(rs.getString("Host_Name"));
		host.setTaxon(rs.getInt("Host_taxon"));
		rec.setHost(host);
		return rec;
	}

}
