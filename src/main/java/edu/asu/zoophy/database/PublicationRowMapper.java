package edu.asu.zoophy.database;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import edu.asu.zoophy.genbank.Publication;

/**
 * Maps SQL row to PubMed Publication
 * @author devdemetri
 */
public class PublicationRowMapper implements RowMapper<Publication> {

	@Override
	public Publication mapRow(ResultSet rs, int rowNum) throws SQLException {
		Publication pub = new Publication();
		pub.setAuthors(rs.getString("Authors"));
		pub.setCentralID(rs.getString("Pubmed_Central_ID"));
		pub.setJournal(rs.getString("Journal"));
		pub.setPubMedID(rs.getInt("Pubmed_ID"));
		pub.setTitle(rs.getString("Title"));
		return pub;
	}

}
