package com.zoophy.database;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.zoophy.genbank.GenBankRecord;
import com.zoophy.genbank.Location;

/**
 * Responsible for retrieving data from the SQL database.
 * @author devdemetri
 */
@Repository("ZoophyDAO")
public class ZoophyDAO {
	
	@Autowired
    private JdbcTemplate jdbc;
	
	private static final String pullRecordDetails = "SELECT \"Sequence_Details\".\"Accession\", \"Collection_Date\", \"Comment\", \"Definition\", \"Isolate\", \"Tax_ID\", \"Organism\", \"Strain\", \"Sequence\", \"Segment_Length\", \"Host_Name\", \"Host_taxon\", \"Geoname_ID\", \"Location\", \"Latitude\", \"Longitude\", \"Type\" FROM \"Sequence_Details\" JOIN \"Host\" ON \"Sequence_Details\".\"Accession\"=\"Host\".\"Accession\" JOIN \"Location_Geoname\" ON \"Sequence_Details\".\"Accession\"=\"Location_Geoname\".\"Accession\" JOIN \"Sequence\" ON \"Sequence_Details\".\"Accession\"=\"Sequence\".\"Accession\" WHERE \"Sequence_Details\".\"Accession\"=?";
	private static final String pullRecordGenes = "SELECT \"Accession\", \"Normalized_Gene_Name\" FROM \"Gene\" WHERE \"Accession\"=?";
	private static final String pullRecordPublication = "SELECT \"Accession\", \"Pubmed_ID\", \"Pubmed_Central_ID\", \"Authors\", \"Title\", \"Journal\" FROM \"Sequence_Publication\" JOIN \"Publication\" ON \"Sequence_Publication\".\"Pub_ID\"=\"Publication\".\"Pubmed_ID\" WHERE \"Accession\"=?";
	private static final String pullRecordLocation = "SELECT \"Accession\", \"Geoname_ID\", \"Location\", \"Latitude\", \"Longitude\", \"Type\" FROM \"Location_Geoname\" WHERE \"Accession\"=?";
	
	/**
	 * Retrieve the specified GenBankRecord from the database without Gene or Publication details
	 * @param accession - unique accession of record to be returned
	 * @return - retrieved GenBankRecord from database
	 * @throws GenBankRecordNotFoundException 
	 * @throws DaoException 
	 */
	public GenBankRecord retrieveLightRecord(String accession) throws GenBankRecordNotFoundException, DaoException {
		try {
			GenBankRecord record = null;
			final String[] param = {accession};
			try {
				record = jdbc.queryForObject(pullRecordDetails, param, new GenBankRecordRowMapper());
			}
			catch (EmptyResultDataAccessException erdae) {
				throw new GenBankRecordNotFoundException(accession);
			}
			return record;
		}
		catch (Exception e) {
			if (e.getClass() != GenBankRecordNotFoundException.class) {
				throw new DaoException(e.getMessage());
			}
			else {
				throw e;
			}
		}
	}
	
	/**
	 * Retrieve the specified GenBankRecord from the database with all related details
	 * @param accession - unique accession of record to be returned
	 * @return - retrieved GenBankRecord from database
	 * @throws GenBankRecordNotFoundException 
	 * @throws DaoException 
	 */
	public GenBankRecord retrieveFullRecord(String accession) throws GenBankRecordNotFoundException, DaoException {
		try {
			GenBankRecord record = null;
			final String[] param = {accession};
			try {
				record = jdbc.queryForObject(pullRecordDetails, param, new GenBankRecordRowMapper());
				record.setGenes(jdbc.query(pullRecordGenes, param, new GeneRowMapper()));
				record.setPublication(jdbc.queryForObject(pullRecordPublication, param, new PublicationRowMapper()));
			}
			catch (EmptyResultDataAccessException erdae) {
				if (record == null) {
					throw new GenBankRecordNotFoundException(accession);
				}
			}
			return record;
		}
		catch (Exception e) {
			if (e.getClass() != GenBankRecordNotFoundException.class) {
				throw new DaoException(e.getMessage());
			}
			else {
				throw e;
			}
		}
	}

	/**
	 * Retrieve the specified record's location from the database
	 * @param accession
	 * @return Location of the accession
	 * @throws GenBankRecordNotFoundException
	 * @throws DaoException
	 */
	public Location retrieveLocation(String accession) throws GenBankRecordNotFoundException, DaoException {
		try {
			Location loc = null;
			final String[] param = {accession};
			try {
				loc = jdbc.queryForObject(pullRecordLocation, param, new LocationRowMapper());
			}
			catch (EmptyResultDataAccessException erdae) {
				throw new GenBankRecordNotFoundException(accession);
			}
			return loc;
		}
		catch (Exception e) {
			if (e.getClass() != GenBankRecordNotFoundException.class) {
				throw new DaoException(e.getMessage());
			}
			else {
				throw e;
			}
		}
	}
	
}
