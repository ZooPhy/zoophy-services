package edu.asu.zoophy.rest.index;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import edu.asu.zoophy.rest.genbank.Location;
import edu.asu.zoophy.rest.security.SecurityHelper;

/**
 * Responsible for retrieving information from Lucene index for Geonames
 * @author amagge
 */
@Repository("LuceneGeonamesSearcher")
public class LuceneGeonamesSearcher {
	
	private Directory indexDirectory;
	private static final FieldType LONG_FIELD_TYPE_STORED_SORTED = new FieldType();
	static {
		LONG_FIELD_TYPE_STORED_SORTED.setTokenized(true);
		LONG_FIELD_TYPE_STORED_SORTED.setOmitNorms(true);
		LONG_FIELD_TYPE_STORED_SORTED.setIndexOptions(IndexOptions.DOCS);
		LONG_FIELD_TYPE_STORED_SORTED.setNumericType(FieldType.NumericType.LONG);
		LONG_FIELD_TYPE_STORED_SORTED.setStored(true);
		LONG_FIELD_TYPE_STORED_SORTED.setDocValuesType(DocValuesType.NUMERIC);
		LONG_FIELD_TYPE_STORED_SORTED.freeze();
	}

	private final static Logger log = Logger.getLogger("LuceneGeonamesSearcher");
	
	public LuceneGeonamesSearcher(@Value("${lucene.geonames.index.location}") String indexLocation) throws LuceneSearcherException {
		try {
			Path index = Paths.get(indexLocation);
			indexDirectory = FSDirectory.open(index);
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
		log.info("testing ");
		Set<String> testAccessions = new HashSet<String>();
		testAccessions.add("5317058");
		testAccessions.add("5308655");
		try {
			Map<String, Location> testList = searchIndex(testAccessions);
			if (testList.keySet().size() != 2) {
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
	 * Search Lucene Index for matching Geonames Records
	 * @param geonameIds - valid Lucene query string
	 * @return Top Lucene query results as a Map of geonameID -> Location objects
	 * @throws LuceneSearcherException 
	 * @throws InvalidLuceneQueryException 
	 */
	public Map<String, Location> searchIndex(Set<String> geonameIds) throws LuceneSearcherException {
		Map<String, Location> records = new HashMap<String, Location>();
		IndexReader reader = null;
		IndexSearcher indexSearcher = null;
		Query query;
		TopDocs documents;
		try {
			reader = DirectoryReader.open(indexDirectory);
			indexSearcher = new IndexSearcher(reader);
			SortField field = new SortField("population", SortField.Type.LONG, true);
			Sort sort = new Sort(field);

			for(String geonameId : geonameIds){
				QueryParser queryParser;
				Pattern geoIdRegex = Pattern.compile(SecurityHelper.FASTA_MET_GEOID_REGEX);
				Matcher geoIdMatcher = geoIdRegex.matcher(geonameId);
				if(geoIdMatcher.matches()){
					queryParser = new QueryParser("GeonameID", new KeywordAnalyzer());;
				} else {
					queryParser = new QueryParser("Location", new StandardAnalyzer());;
				}
				query = queryParser.parse(geonameId);
//				documents = indexSearcher.search(query, 1);
				documents = indexSearcher.search(query, 1, sort);
				for (ScoreDoc scoreDoc : documents.scoreDocs) {
					Document document = indexSearcher.doc(scoreDoc.doc);
					records.put(geonameId, GeonamesDocumentMapper.mapRecord(document));
				}
				log.info("Searching " + geonameId + " query " + query + " . Found " + documents.totalHits);
			}
			return records;
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
	public Set<Long> findLocationAncestors(String geonameId) throws LuceneSearcherException {
		Set<Long> ancestors = new HashSet<Long>();
		IndexReader reader = null;
		IndexSearcher indexSearcher = null;
		Query query;
		TopDocs documents;
		//String querystring = "Accession:"+accession;
		try {
			
			reader = DirectoryReader.open(indexDirectory);
			indexSearcher = new IndexSearcher(reader);
			SortField field = new SortField("population", SortField.Type.LONG, true);
			Sort sort = new Sort(field);

			QueryParser queryParser;
			Pattern geoIdRegex = Pattern.compile(SecurityHelper.FASTA_MET_GEOID_REGEX);
			Matcher geoIdMatcher = geoIdRegex.matcher(geonameId);
			if(geoIdMatcher.matches()){
				queryParser = new QueryParser("GeonameID", new KeywordAnalyzer());;
			} else {
				queryParser = new QueryParser("Location", new StandardAnalyzer());;
			}
			query = queryParser.parse(geonameId);
//			documents = indexSearcher.search(query, 1);
			documents = indexSearcher.search(query, 1, sort);
			Document document = indexSearcher.doc(documents.scoreDocs[0].doc);
			log.info("documents: "+documents.scoreDocs[0].doc);
			for (IndexableField iField : document.getFields("GeonameID")) {
				ancestors.add(Long.parseLong(iField.stringValue()));
				log.info("ansestor: "+iField.stringValue());
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


}
