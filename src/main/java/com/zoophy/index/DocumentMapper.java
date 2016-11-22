package com.zoophy.index;

import org.apache.lucene.document.Document;

import com.zoophy.genbank.GenBankRecord;

/**
 * @author devdemetri
 * Maps Lucene Documents to Java Objects
 */
public class DocumentMapper {
	
	/**
	 * Maps Lucene Document to GenBankRecord 
	 * @param doc - Lucene Document
	 * @throws LuceneSearcherException
	 */
	public static GenBankRecord mapRecord(Document doc) throws LuceneSearcherException {
		GenBankRecord gbr = new GenBankRecord();
		try {
			gbr.setAccession(doc.getField("Accession").stringValue()); 
			return gbr;
		}
		catch (Exception e) {
			throw new LuceneSearcherException("Failed to map document to record: "+e.getMessage());
		}
	}
	
}
