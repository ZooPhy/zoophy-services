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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.fst.Util.TopResults;
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
	private final static Logger log = Logger.getLogger("LuceneSearcher");
	
	public LuceneSearcher(@Value("${lucene.genbank.index.location}") String indexLocation) throws LuceneSearcherException {
		try {
			Path index = Paths.get(indexLocation);
			indexDirectory = FSDirectory.open(index);
			queryParser = new QueryParser("Accession", new StandardAnalyzer());
			log.info("Connected to Index at: "+indexLocation);
		}
		catch (IOException ioe) {
			log.log(Level.SEVERE, "Could not open Lucene Index at: "+indexLocation+ " : "+ioe.getMessage());
			throw new LuceneSearcherException("Could not open Lucene Index at: "+indexLocation+ " : "+ioe.getMessage());
		}
	}
	
	/**
	 * Tests connection to Lucene Index
	 * @throws LuceneSearcherException
	 */
	@PostConstruct
	private void testIndex() throws LuceneSearcherException {
		try {
			List<GenBankRecord> testList = searchIndex("OrganismID:197911", 100);
			if (testList.size() != 100) {
				throw new LuceneSearcherException("Test query should have retrieved 100 records, instead retrieved: "+testList.size());
			}
			log.info("Successfully Tested Lucene Connection.");
		}
		catch (LuceneSearcherException lse) {
			log.log(Level.SEVERE, "Failed to connect to Lucene Index: "+lse.getMessage());
			throw lse;
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Failed to connect to Lucene Index: "+e.getMessage());
			throw new LuceneSearcherException("Failed to connect to Lucene Index: "+e.getMessage());
		}
	}
	
	/**
	 * Closes Lucene resources
	 */
	@PreDestroy
	private void close() {
		try {
			indexDirectory.close();
			log.info("Lucene Index closed");
		}
		catch (IOException ioe) {
			log.warning("Issue closing Lucene Index: "+ioe.getMessage());
		}
	}
	
	/**
	 * Search Lucene Index for count of matching GenBank Records
	 * @param querystring - valid Lucene query string
	 * @return count of search records
	 * @throws LuceneSearcherException 
	 */
	public String searchCount(String queryString) throws LuceneSearcherException {
		IndexReader reader = null;
		IndexSearcher indexSearcher = null;
		Query query;
		
		try{
			TotalHitCountCollector collector = new TotalHitCountCollector();
			reader = DirectoryReader.open(indexDirectory);
			indexSearcher = new IndexSearcher(reader);
			query = queryParser.parse(queryString);
			indexSearcher.search(query, collector);
			return String.valueOf(collector.getTotalHits());
		}catch(Exception e) {
			throw new LuceneSearcherException(e.getMessage());
		}	
	}

	/**
	 * Search Lucene Index for matching GenBank Records
	 * @param querystring - valid Lucene query string
	 * @param maxRecords - maximum results 
	 * @return Top Lucene query results as a List of GenBankRecord objects
	 * @throws LuceneSearcherException 
	 * @throws InvalidLuceneQueryException 
	 */
	public List<GenBankRecord> searchIndex(String querystring, int maxRecords) throws LuceneSearcherException, InvalidLuceneQueryException {
		List<GenBankRecord> records = new LinkedList<GenBankRecord>();
		IndexReader reader = null;
		IndexSearcher indexSearcher = null;
		Query query;
		TopDocs documents;
		
		try {
			Sort sort = new Sort(new SortField("NormalizedDate", SortField.Type.STRING, true));
			reader = DirectoryReader.open(indexDirectory);
			indexSearcher = new IndexSearcher(reader);
			query = queryParser.parse(querystring);
			log.info("query: " + querystring + " : " + querystring);
			documents = indexSearcher.search(query, maxRecords);
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
