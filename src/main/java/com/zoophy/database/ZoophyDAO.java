package com.zoophy.database;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.zoophy.genbank.GenBankRecord;
import com.zoophy.genbank.Gene;


/**
 * Responsible for retreiving data from the SQL database.
 * @author devdemetri
 */
@Repository("ZoophyDAO")
public class ZoophyDAO {
	
	@Autowired
    private JdbcTemplate jdbc;
	
	private static final String pullRecordDetails = "SELECT \"Sequence_Details\".\"Accession\", \"Collection_Date\", \"Comment\", \"Definition\", \"Isolate\", \"Tax_ID\", \"Organism\", \"Strain\", \"Sequence\", \"Segment_Length\", \"Host_Name\", \"Host_taxon\", \"Geoname_ID\", \"Location\", \"Latitude\", \"Longitude\", \"Type\" FROM \"Sequence_Details\" JOIN \"Host\" ON \"Sequence_Details\".\"Accession\"=\"Host\".\"Accession\" JOIN \"Location_Geoname\" ON \"Sequence_Details\".\"Accession\"=\"Location_Geoname\".\"Accession\" JOIN \"Sequence\" ON \"Sequence_Details\".\"Accession\"=\"Sequence\".\"Accession\" WHERE \"Sequence_Details\".\"Accession\"=?";
	private static final String pullRecordGenes = "SELECT \"Gene_ID\", \"Accession\", \"Normalized_Gene_Name\" FROM \"Gene\" WHERE \"Accession\"=?";
	private static final String pullRecordPublication = "SELECT \"Accession\", \"Pubmed_ID\", \"Pubmed_Central_ID\", \"Authors\", \"Title\", \"Journal\" FROM \"Sequence_Publication\" JOIN \"Publication\" ON \"Sequence_Publication\".\"Pub_ID\"=\"Publication\".\"Pubmed_ID\" WHERE \"Accession\"=?";
	
	/**
	 * Retrieve the specified GenBankRecord from the database without Gene or Publication details
	 * @param accession - unique accession of record to be returned
	 * @return - retrieved GenBankRecord from database
	 */
	@Transactional(readOnly=true)
	public GenBankRecord retreiveLightRecord(String accession) {
		GenBankRecord record = jdbc.queryForObject(pullRecordDetails, new Object[] { accession }, new GenBankRecordRowMapper());
		return record;
	}
	
	/**
	 * Retrieve the specified GenBankRecord from the database with all related details
	 * @param accession - unique accession of record to be returned
	 * @return - retrieved GenBankRecord from database
	 */
	@Transactional(readOnly=true)
	public GenBankRecord retreiveFullRecord(String accession) {
		GenBankRecord record = jdbc.queryForObject(pullRecordDetails, new Object[] { accession }, new GenBankRecordRowMapper());
		record.setGenes(jdbc.queryForList(pullRecordGenes, Gene.class, new Object[] { accession }, new GeneRowMapper()));
		record.setPub(jdbc.queryForObject(pullRecordPublication, new Object[] { accession }, new PublicationRowMapper()));
		return record;
	}
	
}
