package com.zoophy.database;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.zoophy.genbank.GenBankRecord;
import com.zoophy.genbank.Host;
import com.zoophy.genbank.Location;
import com.zoophy.genbank.Sequence;

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
		seq.setDefinition("Definition");
		seq.setIsolate(rs.getString("Isolate"));
		seq.setOrganism(rs.getString("Organism"));
		seq.setSegmentLength(rs.getInt("Segment_Length"));
		seq.setRawSequence("Sequence");
		seq.setStrain(rs.getString("Strain"));
		seq.setTaxId(rs.getInt("Tax_ID"));
		rec.setSequence(seq);
		Location loc = new Location();
		loc.setAccession(acc);
		loc.setId(rs.getLong("Geoname_ID"));
		loc.setLocation(rs.getString("Location"));
		loc.setGeonameType(rs.getString("Type"));
		loc.setLatitude(rs.getDouble("Latitude"));
		loc.setLongitude(rs.getDouble("Longitude"));
		rec.setGeonameLocation(loc);
		Host host = new Host();
		host.setAccession(acc);
		host.setName(rs.getString("Host_Name"));
		host.setTaxon(rs.getInt("Host_taxon"));
		rec.setHost(host);
		return rec;
	}

}
