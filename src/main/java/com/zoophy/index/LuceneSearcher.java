package com.zoophy.index;

import java.util.LinkedList;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.zoophy.genbank.GenBankRecord;

/**
 * Responsible for retreiving information from Lucene
 * @author devdemetri
 */
@Repository
public class LuceneSearcher {
	
	/**
	 * @param query - valid Lucene querystring
	 * @return Lucene query results as a List of GenBankRecord objects
	 */
	public List<GenBankRecord> searchIndex(String query) {
		List<GenBankRecord> recs = new LinkedList<GenBankRecord>();
		return recs;
	}
	
}
