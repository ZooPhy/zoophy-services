package edu.asu.zoophy.rest.index;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import edu.asu.zoophy.rest.genbank.GenBankRecord;

/**
 * Responsible for retrieving information from Lucene
 * @author devdemetri
 */
@Repository("LuceneSearcher")
public class LuceneSearcher {
	
	private Directory indexDirectory;
	private QueryParser queryParser;
	private Logger log = Logger.getLogger("LuceneSearcher");
	
	public LuceneSearcher(@Value("${lucene.index.location}") String indexLocation) throws LuceneSearcherException {
		try {
			Path index = Paths.get(indexLocation);
			indexDirectory = FSDirectory.open(index);
			queryParser = new QueryParser("Accession", new KeywordAnalyzer());
			log.info("Connected to Index at: "+indexLocation);
		}
		catch (IOException ioe) {
			log.log(Level.SEVERE, "Could not open Lucene Index at: "+indexLocation+ " : "+ioe.getMessage());
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
		IndexReader reader = null;
		IndexSearcher indexSearcher = null;
		Query query;
		TopDocs documents;
		try {
			reader = DirectoryReader.open(indexDirectory);
			indexSearcher = new IndexSearcher(reader);
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
				if (reader != null) {
					reader.close();
				}
			}
			catch (IOException ioe) {
				log.warning("Could not close IndexReader: "+ioe.getMessage()); 
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
		IndexReader reader = null;
		IndexSearcher indexSearcher = null;
		Query query;
		TopDocs documents;
		String querystring = "Accession:"+accession;
		try {
			reader = DirectoryReader.open(indexDirectory);
			indexSearcher = new IndexSearcher(reader);
			query = queryParser.parse(querystring);
			documents = indexSearcher.search(query, 1);
			if (documents.scoreDocs != null && documents.scoreDocs.length == 1) {
				Document document = indexSearcher.doc(documents.scoreDocs[0].doc);
				for (IndexableField field : document.getFields("GeonameID")) {
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
				if (reader != null) {
					reader.close();
				}
			}
			catch (IOException ioe) {
				log.warning("Could not close IndexReader: "+ioe.getMessage()); 
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
		IndexReader reader = null;
		Query query;
		TopDocs documents;
		String querystring = "Accession:"+accession;
		try {
			reader = DirectoryReader.open(indexDirectory);
			indexSearcher = new IndexSearcher(reader);
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
				if (reader != null) {
					reader.close();
				}
			}
			catch (IOException ioe) {
				log.warning("Could not close IndexReader: "+ioe.getMessage()); 
			}
		}
	}
	
}
