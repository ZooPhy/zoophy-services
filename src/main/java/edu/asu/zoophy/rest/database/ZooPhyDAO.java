package edu.asu.zoophy.rest.database;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import edu.asu.zoophy.rest.genbank.GenBankRecord;
import edu.asu.zoophy.rest.genbank.Location;
import edu.asu.zoophy.rest.pipeline.glm.Predictor;

/**
 * Responsible for retrieving data from the SQL database.
 * @author devdemetri
 */
@Repository("ZoophyDAO")
public class ZooPhyDAO {
	
	@Autowired
    private JdbcTemplate jdbc;
	
	private static final String PULL_RECORD_DETAILS = "SELECT \"Sequence_Details\".\"Accession\", \"Collection_Date\", \"Comment\", \"Definition\", \"Isolate\", \"Tax_ID\", \"Organism\", \"Strain\", \"Sequence\", \"Segment_Length\", \"pH1N1\", \"Host_Name\", \"Host_taxon\", \"Geoname_ID\", \"Location\", \"Latitude\", \"Longitude\", \"Type\", \"Country\" FROM \"Sequence_Details\" JOIN \"Host\" ON \"Sequence_Details\".\"Accession\"=\"Host\".\"Accession\" JOIN \"Location_Geoname\" ON \"Sequence_Details\".\"Accession\"=\"Location_Geoname\".\"Accession\" JOIN \"Sequence\" ON \"Sequence_Details\".\"Accession\"=\"Sequence\".\"Accession\" WHERE \"Sequence_Details\".\"Accession\"=?";
	private static final String PULL_RECORD_GENES = "SELECT \"Accession\", \"Normalized_Gene_Name\" FROM \"Gene\" WHERE \"Accession\"=?  AND \"Normalized_Gene_Name\" IS NOT NULL";
	private static final String PULL_RECORD_PUBLICATION = "SELECT \"Accession\", \"Pubmed_ID\", \"Pubmed_Central_ID\", \"Authors\", \"Title\", \"Journal\" FROM \"Sequence_Publication\" JOIN \"Publication\" ON \"Sequence_Publication\".\"Pub_ID\"=\"Publication\".\"Pubmed_ID\" WHERE \"Accession\"=?";
	private static final String PULL_RECORD_LOCATION = "SELECT \"Accession\", \"Geoname_ID\", \"Location\", \"Latitude\", \"Longitude\", \"Type\", \"Country\" FROM \"Location_Geoname\" WHERE \"Accession\"=?";
	private static final String PULL_STATE_PREDICTORS = "SELECT \"Key\", \"Value\", \"State\", \"Year\" FROM \"Predictor\" WHERE \"State\"=?";
	private static final String TEST_QUERY = "SELECT DISTINCT(\"Accession\") FROM \"Sequence_Details\" LIMIT 500";
	
	private Logger log = Logger.getLogger("ZooPhyDAO");
	
	/**
	 * Tests connection to SQL Database
	 * @throws DaoException
	 */
	@PostConstruct 
	private void testConnection() throws DaoException {
		try {
			List<String> testAccessions = jdbc.queryForList(TEST_QUERY, String.class);
			if (testAccessions.size() != 500) {
				throw new DaoException("Expected 500 Accessions, received: "+testAccessions.size());
			}
			log.info("SQL Database Connection Successfully tested.");
		}
		catch (DaoException de) {
			log.log(Level.SEVERE, "Connection Test Failed: "+de.getMessage());
			throw de;
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Connection Test Failed: "+e.getMessage());
			throw new DaoException("Connection Test Failed: "+e.getMessage());
		}
	}

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
			final String[] parameters = {accession};
			try {
				record = jdbc.queryForObject(PULL_RECORD_DETAILS, parameters, new GenBankRecordRowMapper());
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
			final String[] parameters = {accession};
			try {
				record = jdbc.queryForObject(PULL_RECORD_DETAILS, parameters, new GenBankRecordRowMapper());
				record.setGenes(jdbc.query(PULL_RECORD_GENES, parameters, new GeneRowMapper()));
				try {
					record.setPublication(jdbc.queryForObject(PULL_RECORD_PUBLICATION, parameters, new PublicationRowMapper()));
				}
				catch (EmptyResultDataAccessException erdae){}
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
	 * Retrieve the specified record's location from the database
	 * @param accession
	 * @return Location of the accession
	 * @throws GenBankRecordNotFoundException
	 * @throws DaoException
	 */
	public Location retrieveLocation(String accession) throws GenBankRecordNotFoundException, DaoException {
		try {
			Location location = null;
			final String[] param = {accession};
			try {
				location = jdbc.queryForObject(PULL_RECORD_LOCATION, param, new LocationRowMapper());
			}
			catch (EmptyResultDataAccessException erdae) {
				throw new GenBankRecordNotFoundException(accession);
			}
			return location;
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
	 * Retrieve the specified US State's GLM predictors from the database
	 * @param state - US State to retrieve default predictors
	 * @return list of Predictors for given US State
	 * @throws DaoException
	 */
	public List<Predictor> retrieveDefaultPredictors(String state) throws DaoException {
		try {
			final String[] parameters = {state};
			List<Predictor> predictors = jdbc.query(PULL_STATE_PREDICTORS, parameters, new PredictorRowMapper());
			return predictors;
		}
		catch (Exception e) {
			throw new DaoException(e.getMessage());
		}
	}
	
}
