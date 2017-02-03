package edu.asu.zoophy.rest.database;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import edu.asu.zoophy.rest.genbank.Publication;

/**
 * Maps SQL row to PubMed Publication
 * @author devdemetri
 */
public class PublicationRowMapper implements RowMapper<Publication> {

	@Override
	public Publication mapRow(ResultSet row, int rowNumber) throws SQLException {
		Publication publication = new Publication();
		publication.setAuthors(row.getString("Authors"));
		publication.setCentralID(row.getString("Pubmed_Central_ID"));
		publication.setJournal(row.getString("Journal"));
		publication.setPubMedID(row.getInt("Pubmed_ID"));
		publication.setTitle(row.getString("Title"));
		return publication;
	}

}
