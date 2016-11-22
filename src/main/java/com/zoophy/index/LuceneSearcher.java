package com.zoophy.index;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.springframework.stereotype.Repository;

import com.zoophy.genbank.GenBankRecord;

/**
 * Responsible for retreiving information from Lucene
 * @author devdemetri
 */
@Repository
public class LuceneSearcher {
	
	private static final String indexLocation = "/home/devdemetri/Desktop/Big_Index";
	private static Directory indexDirectory;
	private static QueryParser queryParser;
	
	public LuceneSearcher() throws LuceneSearcherException {
		try {
			indexDirectory = FSDirectory.open(new File(indexLocation));
			queryParser = new QueryParser(Version.LUCENE_30, "text", new StandardAnalyzer(Version.LUCENE_30));
		}
		catch (IOException ioe) {
			throw new LuceneSearcherException("Could not open Lucene Index at: "+indexLocation);
		}
	}
	
	/**
	 * @param query - valid Lucene querystring
	 * @return Top 10000 Lucene query results as a List of GenBankRecord objects
	 * @throws LuceneSearcherException 
	 * @throws InvalidLuceneQueryException 
	 */
	public List<GenBankRecord> searchIndex(String querystring) throws LuceneSearcherException, InvalidLuceneQueryException {
		List<GenBankRecord> recs = new LinkedList<GenBankRecord>();
		IndexSearcher indexSearcher = null;
		Query query;
		TopDocs docs;
		try {
			indexSearcher = new IndexSearcher(indexDirectory);
			query = queryParser.parse(querystring);
			docs = indexSearcher.search(query, 10000);
			for (ScoreDoc scoreDoc : docs.scoreDocs) {
				Document doc = indexSearcher.doc(scoreDoc.doc);
				recs.add(DocumentMapper.mapRecord(doc));
			}
			return recs;
		}
		catch (ParseException pe) {
			throw new InvalidLuceneQueryException(pe.getMessage());
		}
		catch (Exception e) {
			throw new LuceneSearcherException(e.getMessage());
		}
		finally {
			try {
				indexSearcher.close();
			} 
			catch (IOException ioe) {
				//just going to ignore this for now...
			}
		}
	}
	
}
