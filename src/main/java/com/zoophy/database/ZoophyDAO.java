package com.zoophy.database;

import org.springframework.stereotype.Repository;

import com.zoophy.genbank.GenBankRecord;


/**
 * Responsible for retreiving data from the SQL database.
 * @author devdemetri
 */
@Repository("ZoophyDAO")
public class ZoophyDAO {
	
	/**
	 * Retrieve the specified GenBankRecord from the database
	 * @param accession - unique accession of record to be returned
	 * @return - retrieved GenBankRecord from database
	 */
	public GenBankRecord retreiveRecord(String accession) {
		
		
		return null;
	}
	
}
