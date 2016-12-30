package edu.asu.zoophy.index;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import edu.asu.zoophy.genbank.GenBankRecord;

/**
 * Responsible for retrieving information from Lucene
 * @author devdemetri
 */
@Repository("LuceneSearcher")
public class LuceneSearcher {
	
	private Directory indexDirectory;
	private QueryParser queryParser;
	
	public LuceneSearcher(@Value("${lucene.index.location}") String indexLocation) throws LuceneSearcherException {
		try {
			File index = new File(indexLocation);
			if (index.exists() && index.isDirectory()) {
				indexDirectory = FSDirectory.open(index);
				IndexSearcher indexSearcher = new IndexSearcher(indexDirectory, true);
				indexSearcher.close();
			}
			else {
				throw new LuceneSearcherException("No Lucene Index at: "+indexLocation);
			}
			queryParser = new QueryParser(Version.LUCENE_30, "text", new KeywordAnalyzer());
		}
		catch (IOException ioe) {
			throw new LuceneSearcherException("Could not open Lucene Index at: "+indexLocation+ " : "+ioe.getMessage());
		}
	}
	
	/**
	 * @param query - valid Lucene query string
	 * @return Top 2500 Lucene query results as a List of GenBankRecord objects
	 * @throws LuceneSearcherException 
	 * @throws InvalidLuceneQueryException 
	 */
	public List<GenBankRecord> searchIndex(String querystring) throws LuceneSearcherException, InvalidLuceneQueryException {
		List<GenBankRecord> records = new LinkedList<GenBankRecord>();
		IndexSearcher indexSearcher = null;
		Query query;
		TopDocs documents;
		try {
			indexSearcher = new IndexSearcher(indexDirectory, true);
			query = queryParser.parse(querystring);
			documents = indexSearcher.search(query, 2500);
			for (ScoreDoc scoreDoc : documents.scoreDocs) {
				Document document = indexSearcher.doc(scoreDoc.doc);
				records.add(DocumentMapper.mapRecord(document));
			}
			return records;
		}
		catch (ParseException pe) {
			throw new InvalidLuceneQueryException(pe.getMessage());
		}
		catch (Exception e) {
			throw new LuceneSearcherException(e.getMessage());
		}
		finally {
			try {
				if (indexSearcher != null) {
					indexSearcher.close();
				}
			}
			catch (IOException ioe) {
				//just going to ignore this for now...
			}
		}
	}

	/**
	 * Finds the Set of ancestors for a record's Geoname location
	 * @param accession - Accession of record to check
	 * @return Set of ancestors for a record's Geoname location
	 * @throws LuceneSearcherException
	 */
	public Set<Long> findLocationAncestors(String accession) throws LuceneSearcherException {
		Set<Long> ancestors = new HashSet<Long>();
		IndexSearcher indexSearcher = null;
		Query query;
		TopDocs documents;
		String querystring = "Accession:"+accession;
		try {
			indexSearcher = new IndexSearcher(indexDirectory, true);
			query = queryParser.parse(querystring);
			documents = indexSearcher.search(query, 1);
			if (documents.scoreDocs != null && documents.scoreDocs.length == 1) {
				Document document = indexSearcher.doc(documents.scoreDocs[0].doc);
				for (Field field : document.getFields("GeonameID")) {
					ancestors.add(Long.parseLong(field.stringValue()));
				}
			}
			return ancestors;
		}
		catch (Exception e) {
			throw new LuceneSearcherException(e.getMessage());
		}
		finally {
			try {
				if (indexSearcher != null) {
					indexSearcher.close();
				}
			}
			catch (IOException ioe) {
				//just going to ignore this for now...
			}
		}
	}

	/**
	 * Retrieves a single GenBankRecord from the Index
	 * @param accession
	 * @return The GenBankRecord from the Index if it exists, otherwise null
	 * @throws LuceneSearcherException
	 */
	public GenBankRecord getRecord(String accession) throws LuceneSearcherException {
		IndexSearcher indexSearcher = null;
		Query query;
		TopDocs documents;
		String querystring = "Accession:"+accession;
		try {
			indexSearcher = new IndexSearcher(indexDirectory, true);
			query = queryParser.parse(querystring);
			documents = indexSearcher.search(query, 1);
			if (documents.scoreDocs != null && documents.scoreDocs.length == 1) {
				Document document = indexSearcher.doc(documents.scoreDocs[0].doc);
				return DocumentMapper.mapRecord(document);
			}
			else {
				return null;
			}
		}
		catch (Exception e) {
			throw new LuceneSearcherException(e.getMessage());
		}
		finally {
			try {
				if (indexSearcher != null) {
					indexSearcher.close();
				}
			}
			catch (IOException ioe) {
				//just going to ignore this for now...
			}
		}
	}
	
}
