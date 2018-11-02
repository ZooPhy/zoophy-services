package edu.asu.zoophy.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import edu.asu.zoophy.rest.custom.DownloadRecords;
import edu.asu.zoophy.rest.custom.FastaRecord;
import edu.asu.zoophy.rest.database.DaoException;
import edu.asu.zoophy.rest.database.GenBankRecordNotFoundException;
import edu.asu.zoophy.rest.database.ZooPhyDAO;
import edu.asu.zoophy.rest.genbank.GenBankRecord;
import edu.asu.zoophy.rest.genbank.Location;
import edu.asu.zoophy.rest.genbank.PossibleLocation;
import edu.asu.zoophy.rest.genbank.JobAccessions;
import edu.asu.zoophy.rest.index.InvalidLuceneQueryException;
import edu.asu.zoophy.rest.index.LuceneHierarchySearcher;
import edu.asu.zoophy.rest.index.LuceneSearcher;
import edu.asu.zoophy.rest.index.LuceneSearcherException;
import edu.asu.zoophy.rest.pipeline.PipelineException;
import edu.asu.zoophy.rest.pipeline.PipelineManager;
import edu.asu.zoophy.rest.pipeline.ZooPhyRunner;
import edu.asu.zoophy.rest.pipeline.glm.GLMException;
import edu.asu.zoophy.rest.pipeline.glm.PredictorTemplateGenerator;
import edu.asu.zoophy.rest.pipeline.utils.DownloadFormat;
import edu.asu.zoophy.rest.pipeline.utils.DownloadFormatter;
import edu.asu.zoophy.rest.pipeline.utils.FormatterException;
import edu.asu.zoophy.rest.security.Parameter;
import edu.asu.zoophy.rest.security.ParameterException;
import edu.asu.zoophy.rest.security.SecurityHelper;

/**
 * Responsible for mapping ZooPhy service requests
 * @author devdemetri, amagge, kbhangal
 */
@RestController
public class ZooPhyController {
	
	@Autowired
	private ZooPhyDAO dao;
	
	@Autowired
	private LuceneSearcher indexSearcher;
	
	@Autowired
	private LuceneHierarchySearcher hierarchyIndexSearcher;

	@Autowired
	private SecurityHelper security;
	
	@Autowired
	private PipelineManager manager;
	
	@Value("${job.max.accessions}")
	private Integer JOB_MAX_ACCESSIONS;
	
	@Value("${query.max.records}")
	private Integer QUERY_MAX_RECORDS;
	
	@Autowired
	private DownloadFormatter formatter;
	
	@Autowired
	private PredictorTemplateGenerator templateGenerator;
	
	private final static Logger log = Logger.getLogger("ZooPhyController");
	
	/**
	 * Simple check that REST services are running
	 * @return message that services are running
	 */
	@RequestMapping(value="/", method=RequestMethod.GET)
    @ResponseStatus(value=HttpStatus.OK)
	public String checkService() {
		return "ZooPhy Services are up and running.";
	}
	
    /**
     * Retrieves the specified record from the database.
     * @param accession - Accession of GenBankRecord to be retrieved
     * @param isFull - does the record need its associated Publication and Genes
     * @return specified record from the database.
     * @throws GenBankRecordNotFoundException
     * @throws DaoException 
     * @throws ParameterException
     */
    @RequestMapping(value="/record", method=RequestMethod.GET)
    @ResponseStatus(value=HttpStatus.OK)
    public GenBankRecord getDatabaseRecord(@RequestParam(value="accession") String accession, @RequestParam(value="isfull", required=false, defaultValue="true") Boolean isFull) throws GenBankRecordNotFoundException, DaoException, ParameterException {
    	if (security.checkParameter(accession, Parameter.ACCESSION)) {
	    	GenBankRecord record = null;
	    	if (isFull) {
	    		log.info("Retrieving full record: "+accession);
	    		record = dao.retrieveFullRecord(accession);
	    	}
	    	else {
	    		log.info("Retrieving light record: "+accession);
	    		record = dao.retrieveLightRecord(accession);
	    	}
	    	log.info("Successfully retrieved record: "+accession);
	    	return record;
    	}
    	else {
    		log.warning("Bad record accession parameter: "+accession);
    		throw new ParameterException(accession);
    	}
    }
    
    /**
     * Returns the Geoname Location for the given Accession
     * @param accession
     * @return Location of the accession
     * @throws DaoException 
     * @throws GenBankRecordNotFoundException 
     * @throws ParameterException 
     */
    @RequestMapping(value="/location", method=RequestMethod.GET)
    @ResponseStatus(value=HttpStatus.OK)
    public Location getRecordLocation(@RequestParam(value="accession") String accession) throws GenBankRecordNotFoundException, DaoException, ParameterException {
    	if (security.checkParameter(accession, Parameter.ACCESSION)) {
    		log.info("Retrieving record location: "+accession);
    		Location location = dao.retrieveLocation(accession);
    		log.info("Successfully retrieved location: "+accession);
    		return location;
    	}
    	else {
    		log.warning("Bad accession parameter: "+accession);
    		throw new ParameterException(accession);
    	}
    }
    
    /**
     * Returns the list of possible locations for the given Accession with their probabilities
     * @param accession
     * @return Location of the accession
     * @throws DaoException 
     * @throws GenBankRecordNotFoundException 
     * @throws ParameterException 
     */
    @RequestMapping(value="/locations", method=RequestMethod.GET)
    @ResponseStatus(value=HttpStatus.OK)
    public List<PossibleLocation> getRecordLocations(@RequestParam(value="accession") String accession) throws GenBankRecordNotFoundException, DaoException, ParameterException {
    	if (security.checkParameter(accession, Parameter.ACCESSION)) {
    		List<PossibleLocation> loc = dao.retrieveLocations(accession);
    		return loc;
    	} else {
    		throw new ParameterException(accession);
    	}
    }
    
    /**
     * Retrieve count of GenBankRecords for resulting Lucene query
     * @param query - Valid Lucene query string
     * @return count Number of  GenBankRecord results of given query.
     * @throws LuceneSearcherException 
     * @throws InvalidLuceneQueryException 
     * @throws ParameterException 
     */
    @RequestMapping(value="/search/count", method=RequestMethod.GET)
    @ResponseStatus(value=HttpStatus.OK)
    public String countqueryLucene(@RequestParam(value="query") String query) throws LuceneSearcherException, InvalidLuceneQueryException, ParameterException {
    	if (security.checkParameter(query, Parameter.LUCENE_QUERY)) {
    		log.info("Searching query: "+query);
    		String count = indexSearcher.searchCount(query);
    		log.info("Successfully searched query: "+query);
    		return count;
    	}
    	else {
    		log.warning("Bad query parameter: "+query);
    		throw new ParameterException(query);
    	}
    }

    /**
     * Retrieve GenBankRecords for resulting Lucene query
     * @param query - Valid Lucene query string
     * @return GenBankRecord results of given query.
     * @throws LuceneSearcherException 
     * @throws InvalidLuceneQueryException 
     * @throws ParameterException 
     */
    @RequestMapping(value="/search", method=RequestMethod.GET)
    @ResponseStatus(value=HttpStatus.OK)
    public List<GenBankRecord> queryLucene(@RequestParam(value="query") String query) throws LuceneSearcherException, InvalidLuceneQueryException, ParameterException {
	    	if (security.checkParameter(query, Parameter.LUCENE_QUERY)) {
	    		log.info("Searching query: "+query);
	    		List<GenBankRecord> results = indexSearcher.searchIndex(query, QUERY_MAX_RECORDS);
	    		log.info("Successfully searched query: "+query);
	    		Collections.sort(results, new Comparator<GenBankRecord>() {
	    		    public int compare(GenBankRecord r1, GenBankRecord r2) {
	    		        return r1.getAccession().compareTo(r2.getAccession());
	    		    }
	    		});
	    		return results;
	    	}
	    	else {
	    		log.warning("Bad query parameter: "+query);
	    		throw new ParameterException(query);
	    	}
    }
    
    /**
     * As a results of HTTP Header size conflicts, this service is meant to query a long specific list of Accession.
     * @param accessions - Accessions to query
     * @return list of index GenBankRecords for the given accessions
     * @throws ParameterException
     * @throws LuceneSearcherException
     * @throws InvalidLuceneQueryException
     */
    @RequestMapping(value="/search/accessions", method=RequestMethod.POST)
    @ResponseStatus(value=HttpStatus.OK)
    public List<GenBankRecord> queryAccessions(@RequestBody List<String> accessions) throws ParameterException, LuceneSearcherException, InvalidLuceneQueryException {
    	List<GenBankRecord> records = null;
    	log.info("Searching accession list...");
    	if (accessions != null && accessions.size() > 0) {
    		if (accessions.size() > QUERY_MAX_RECORDS) {
    			log.warning("Query accession list is too long.");
	    		throw new ParameterException("accessions list is too long");
    		}
    		Set<String> uniqueAccessions = new LinkedHashSet<String>(accessions.size());
    		for (String accession : accessions) {
    			if (security.checkParameter(accession, Parameter.ACCESSION)) {
    				uniqueAccessions.add(accession);
    			}
    			else {
    				log.warning("Bad accession parameter: "+accession);
    				throw new ParameterException(accession);
    			}
    		}
    		List<String> usableAccessions = new LinkedList<String>(uniqueAccessions);
    		uniqueAccessions.clear();
    		records = new LinkedList<GenBankRecord>();
    		final int INDIVIDUAL_QUERY_LIMIT = 1024;
    		int current;
    		while (!usableAccessions.isEmpty()) {
    			current = 0;
	    		StringBuilder queryBuilder = new StringBuilder("Accession: (");
	    		queryBuilder.append(usableAccessions.remove(0));
	    		current++;
	    		while (!usableAccessions.isEmpty() && current < INDIVIDUAL_QUERY_LIMIT) {
	    			queryBuilder.append(" OR ");
	    			queryBuilder.append(usableAccessions.remove(0));
	    			current++;
	    		}
	    		queryBuilder.append(")");
	    		records.addAll(indexSearcher.searchIndex(queryBuilder.toString(), INDIVIDUAL_QUERY_LIMIT));
    		}
    		log.info("Successfully searched accession list.");
    	}
    	else {
    		log.warning("Accession list is empty.");
    		records = new LinkedList<GenBankRecord>();
    	}
    	return records;
    }
    
    /**
     * Validate fasta contents and retrieve FASTA formatted records for display and eventual job run.
     * @param records - fasta records to query
     * @return list of index FastaRecords with locations for the given Fasta
     * @throws ParameterException
     * @throws LuceneSearcherException
     * @throws InvalidLuceneQueryException
     */
    @RequestMapping(value="/upfasta", method=RequestMethod.POST, headers="Accept=application/json")
    @ResponseStatus(value=HttpStatus.ACCEPTED)
    public List<FastaRecord> checkFasta(@RequestBody List<JobRecord> records) throws ParameterException, LuceneSearcherException, InvalidLuceneQueryException {
    	log.info("Checking FASTA list...");
    	List<FastaRecord> fastaRecords = new LinkedList<FastaRecord>();
    	if (records != null && records.size() > 0) {
			log.info("FASTA list size " + records.size());
    		if (records.size() > QUERY_MAX_RECORDS) {
    			log.warning("Query accession list is too long.");
	    		throw new ParameterException("accessions list is too long");
    		}
        	Set<String> geonameIds = new LinkedHashSet<String>(records.size());
        	for(JobRecord record : records) {
				try {
					if(security.checkParameter(record.getGeonameID().toString(), Parameter.LOCATION)){
						geonameIds.add(record.getGeonameID().toString());
					}
				} catch (Exception e) {
					log.warning("Skipping violating Geoname id value " + record.getStringRep());
				}
        	}
    		Map<String, Location> geonamesMap;
    		
    		try {
    			geonamesMap = hierarchyIndexSearcher.findGeonameLocations(geonameIds);
    		} catch (LuceneSearcherException e) {
    			log.warning("Geonames Lucene exception: " + e.getMessage());
    			throw e;
    		}
    		Location loc = new Location();
        	for(JobRecord record : records) {
        		if  (security.checkParameter(record.getId(), Parameter.RECORD_ID) && 
        				security.checkParameter(record.getCollectionDate(), Parameter.DATE) &&
        				security.checkParameter(record.getRawSequence(), Parameter.RAW_SEQUENCE)) {
        			if (geonamesMap.containsKey(record.getGeonameID())){
        				loc = geonamesMap.get(record.getGeonameID());
        				loc.setAccession(record.getId());
        			} else {
        				loc = new Location();
        			}
        			FastaRecord fastaRecord = new FastaRecord(
        											record.getId(), 
        											record.getCollectionDate(), 
        											record.getRawSequence(),
        											loc);
        			fastaRecords.add(fastaRecord);
        		}
        		else {
        			log.warning("Bad parameters for record : "+record.getId());
        			throw new ParameterException(record.getId());
        		}
        	}
    		log.info("Successfully searched accession list.");
    	}
    	else {
    		log.warning("Accession list is empty.");
    	}

    	return fastaRecords;
    }

    /**
     * Run ZooPhy Job
     * @param parameters
     * @return jobID for started ZooPhy Job
     * @throws ParameterException
     * @throws PipelineException
     * @throws LuceneSearcherException 
     */
    @RequestMapping(value="/run", method=RequestMethod.POST, headers="Accept=application/json")
    @ResponseStatus(value=HttpStatus.ACCEPTED)
    public String runZooPhyJob(@RequestBody JobParameters parameters) throws ParameterException, PipelineException, LuceneSearcherException {
    	log.info("Starting ZooPhy job..."+parameters);
    	if (security.checkParameter(parameters.getReplyEmail(), Parameter.EMAIL)) {
    		ZooPhyRunner zoophy;
	    	if (parameters.getJobName() != null) {
	    		if (!security.checkParameter(parameters.getJobName(), Parameter.JOB_NAME)) {
	    			log.warning("Bad job name parameter: "+parameters.getJobName());
	    			throw new ParameterException(parameters.getJobName());
	    		}
	    	}
    		try {
    			security.verifyXMLOptions(parameters.getXmlOptions());
    		}
    		catch (ParameterException pe) {
    			log.warning("Bad XML Parameters: "+pe.getMessage());
    			throw pe;
    		}    		
    		zoophy = new ZooPhyRunner(parameters.getReplyEmail(), parameters.getJobName(), parameters.isUsingGLM(), parameters.getPredictors(), parameters.getXmlOptions());
    		Set<String> genBankJobAccessions = new LinkedHashSet<String>();
    		Set<String> geonameIds = new LinkedHashSet<String>();
    		ArrayList<JobRecord> userEnteredRecords= new ArrayList<>();
    		List<FastaRecord> fastaRecords = new LinkedList<FastaRecord>();
    		
    		for(JobRecord jobrecord: parameters.getRecords()) {
    			if(jobrecord.getResourceSource()==JobConstants.SOURCE_GENBANK) {
    				String accession = jobrecord.getId();
    				if  (security.checkParameter(accession, Parameter.ACCESSION)) {
    					genBankJobAccessions.add(accession);
    				}else {
        				log.warning("Bad accession parameter: "+accession);
        				throw new ParameterException(accession);
        			}
    			}else if(jobrecord.getResourceSource()==JobConstants.SOURCE_FASTA){
    				userEnteredRecords.add(jobrecord);
    			}	
    		}
    		log.info("genBank records: "+genBankJobAccessions.size()+", fasta records: "+userEnteredRecords.size());
    		
    		//userEntered-FASTA
    		if(userEnteredRecords.size()>0) {
	    		for(JobRecord jobrecord: userEnteredRecords) {
	    			if(security.checkParameter(jobrecord.getGeonameID(), Parameter.LOCATION)){
		    			geonameIds.add(jobrecord.getGeonameID());
	    			}
	    		}
	    		log.info("geonameIds: "+geonameIds.size());
	    		Map<String, Location> geonamesMap;
				try {
					geonamesMap = hierarchyIndexSearcher.findGeonameLocations(geonameIds);
				} catch (LuceneSearcherException e) {
	    			log.warning("Geonames Lucene exception: " + e.getMessage());
	    			throw e;
				}
				Location loc = new Location();
		    	for(JobRecord jobrecord: userEnteredRecords) {
		    		if  (security.checkParameter(jobrecord.getId(), Parameter.RECORD_ID) && 
		    				security.checkParameter(jobrecord.getCollectionDate(), Parameter.DATE) &&
		    				security.checkParameter(jobrecord.getRawSequence(), Parameter.RAW_SEQUENCE)) {
		    			if (geonamesMap.containsKey(jobrecord.getGeonameID())){
		    				loc = geonamesMap.get(jobrecord.getGeonameID());
		    				loc.setAccession(jobrecord.getId());
		    			} else {
		    				loc = new Location();
		    			}
		    			FastaRecord fastaRecord = new FastaRecord(
		    					jobrecord.getId(), 
		    					jobrecord.getCollectionDate(), 
		    					jobrecord.getRawSequence(),
		    					loc);
		    			fastaRecords.add(fastaRecord);
		    		}
		    		else {
		    			log.warning("Bad parameters for record : "+jobrecord.getId());
		    			throw new ParameterException(jobrecord.getId());
		    		}
		    	}
    		}
    		if (genBankJobAccessions.size()+fastaRecords.size() > JOB_MAX_ACCESSIONS) {
    			log.warning("Record list is too long.");
    			throw new ParameterException("Record list is too long");
    		}
    		if (genBankJobAccessions.size() > JOB_MAX_ACCESSIONS) {
    			log.warning("Job accession list is too long.");
    			throw new ParameterException("accessions list is too long");
    		}
    		if (fastaRecords.size() > JOB_MAX_ACCESSIONS) {
	    		log.warning("FASTA record list is too long.");
	    		throw new ParameterException("accessions list is too long");
	    	}
    		manager.startZooPhyPipeline(zoophy, new ArrayList<String>(genBankJobAccessions), fastaRecords);
    		log.info("Job successfully started: "+zoophy.getJobID());
    		return zoophy.getJobID();
    		
    	}
    	else {
    		log.warning("Bad reply email parameter: "+parameters.getReplyEmail());
    		throw new ParameterException(parameters.getReplyEmail());
    	}
    }
    
    /**
     * Stop a running ZooPhyJob by the Job ID
     * @param jobID - ID of Job to be stopped
     * @throws PipelineException
     * @throws ParameterException
     */
    @RequestMapping(value="/stop", method=RequestMethod.DELETE)
    @ResponseStatus(value=HttpStatus.OK)
    public String stopZooPhyJob(@RequestParam(value="id") String jobID) throws PipelineException, ParameterException {
    	if (security.checkParameter(jobID, Parameter.JOB_ID)) {
    		log.info("Stopping ZooPhy Job: "+jobID);
    		try {
	    		manager.killJob(jobID);
	    		log.info("Successfully stopped ZooPhy Job: "+jobID);
	    		return "Successfully stopped ZooPhy Job: "+jobID;
    		}
    		catch (PipelineException pe) {
    			log.warning("Failed to stop job: "+jobID+" : "+pe.getMessage());
    			throw new ParameterException(jobID);
    		}
    	}
    	else {
    		log.warning("Bad Job ID parameter: "+jobID);
    		throw new ParameterException(jobID);
    	}
    }
    
    /**
     * Generate the contents of GenBankRecords download in the specified format
     * @param format 
     * @param accessions
     * @return String of GenBankRecords download in the specified format
     * @throws ParameterException
     * @throws FormatterException
     */
    @RequestMapping(value="/download", method=RequestMethod.POST)
    @ResponseStatus(value=HttpStatus.OK)
    public String retrieveDownload(@RequestParam(value="format") String format, @RequestBody DownloadRecords downloadRecords) throws ParameterException, FormatterException {
    	log.info("Setting up download...");
    	List<JobRecord> records  = downloadRecords.getAccessions();  	
    	
    	List<String> columns = downloadRecords.getColumns();
    	if(columns != null && columns.size()>0) {
	    	if (format != null) {
	    		if (records == null || records.size() == 0) {
	    			log.warning("Empty accession list.");
	    			return null;
	    		}
	    		if (records.size() > QUERY_MAX_RECORDS) {
	    			log.warning("Too many accessions.");
	    			throw new ParameterException("accessions list is too long");
	    		}
	    		Set<JobRecord> downloadAccessions = new LinkedHashSet<JobRecord>(records.size());
	    		for (JobRecord record : records) {
	    			if  ((security.checkParameter(record.getId(), Parameter.ACCESSION) && record.getResourceSource() == JobConstants.SOURCE_GENBANK) || record.getResourceSource() == JobConstants.SOURCE_FASTA) {
	    				downloadAccessions.add(record);
		    		}
		    		else {
		    			log.warning("Bad accession parameter: "+record.getId());
		    			throw new ParameterException(record.getId());
		    		}
	    		}
	    		records = new LinkedList<JobRecord>(downloadAccessions);
	    		downloadAccessions.clear();
	    		String download = null;
	    		if (format.equalsIgnoreCase("CSV")) {
	    			log.info("Generating CSV download...");
	    			download = formatter.generateDownload(records, columns, DownloadFormat.CSV);
	    		}
	    		else if (format.equalsIgnoreCase("FASTA")) {
	    			log.info("Generating FASTA download...");
	    			download = formatter.generateDownload(records, columns, DownloadFormat.FASTA);
	    		}
	    		else {
	    			log.warning("Bad format parameter: "+format);
	    			throw new ParameterException(format);
	    		}
	    		log.info("Successfully generated download.");
	    		return download;
	    	}
	    	else {
	    		log.warning("Bad format parameter: "+format);
	    		throw new ParameterException(format);
	    	}
    	}else {
    		log.warning("Too few Columns for download");
    		throw new ParameterException("columns");
    	}
    }
    
    /**
     * Generates a GLM Predictors template for users to fill in. Template already includes lat, long, and SampleSize.
     * @param accessions - Accessions to base template on
     * @return GLM Predictors template
     * @throws ParameterException
     * @throws GLMException
     */
    @RequestMapping(value="/template", method=RequestMethod.POST)
    @ResponseStatus(value=HttpStatus.OK)
    public String retrieveTemplate(@RequestBody List<String> accessions) throws ParameterException, GLMException {
    	try {
	    	log.info("Setting up GLM Predictors template...");
			if (accessions == null || accessions.size() == 0) {
				log.warning("Empty accession list.");
				throw new ParameterException("accessions list is empty");
			}
			if (accessions.size() > 1000) {
				log.warning("Too many accessions.");
				throw new ParameterException("accessions list is too long");
			}
			Set<String> templateAccessions = new LinkedHashSet<String>(accessions.size());
			for (String accession : accessions) {
				if  (security.checkParameter(accession, Parameter.ACCESSION)) {
					templateAccessions.add(accession);
	    		}
	    		else {
	    			log.warning("Bad accession parameter: "+accession);
	    			throw new ParameterException(accession);
	    		}
			}
			accessions = new LinkedList<String>(templateAccessions);
			templateAccessions.clear();
			String template = templateGenerator.generateTemplate(accessions);
			log.info("Successfully generated GLM Predictors template.");
			return template;
    	}
    	catch (ParameterException pe) {
    		throw pe;
    	}
    	catch (GLMException glme) {
    		log.log(Level.SEVERE, "GLM error generating Predictors template:\t"+glme.getMessage());
    		throw glme;
    	}
    }

    /**
     * Validate a ZooPhy Job before starting it, to avoid common failures in the early stages.
     * @param parameters
     * @return Null if the job is valid, otherwise the String of Error message(s).
     * @throws ParameterException
     * @throws PipelineException
     */
    @RequestMapping(value="/validate", method=RequestMethod.POST, headers="Accept=application/json")
    @ResponseStatus(value=HttpStatus.OK)
    public ValidationResults validateNewJob(@RequestBody JobParameters parameters) throws ParameterException, PipelineException {
    	log.info("Validating New job...");
    	ValidationResults results = new ValidationResults();
    	try {
        	if (security.checkParameter(parameters.getReplyEmail(), Parameter.EMAIL)) {
        		ZooPhyRunner zoophy;
    	    	if (parameters.getJobName() != null) {
    	    		if (!security.checkParameter(parameters.getJobName(), Parameter.JOB_NAME)) {
    	    			log.warning("Bad job name parameter: "+parameters.getJobName());
    	    			throw new ParameterException(parameters.getJobName());
    	    		}
    	    	}
        		try {
        			security.verifyXMLOptions(parameters.getXmlOptions());
        		}
        		catch (ParameterException pe) {
        			log.warning("Bad XML Parameters: "+pe.getMessage());
        			throw pe;
        		}
    	    	zoophy = new ZooPhyRunner(parameters.getReplyEmail(), parameters.getJobName(), parameters.isUsingGLM(), parameters.getPredictors(), parameters.getXmlOptions());
    	    Set<String> jobAccessions = new LinkedHashSet<String>();
    	    	Set<String> geonameIds = new LinkedHashSet<String>();
        	ArrayList<JobRecord> userEnteredRecords= new ArrayList<>();
        	List<FastaRecord> fastaRecords = new LinkedList<FastaRecord>();
        	Set<String> jobRecordIds = new HashSet<String>();
    	    	for(JobRecord jobrecord: parameters.getRecords()) {
        			if(jobrecord.getResourceSource() == JobConstants.SOURCE_GENBANK) {
        				String accession = jobrecord.getId();
            			if  (security.checkParameter(accession, Parameter.ACCESSION)) {
                			jobAccessions.add(accession);
            			}else {
                			log.warning("Bad accession parameter: "+accession);
                			throw new ParameterException(accession);
                		}
        			}else if(jobrecord.getResourceSource() == JobConstants.SOURCE_FASTA){
        				userEnteredRecords.add(jobrecord);
        			}
        		}
        	//userEntered-FASTA
    		if(userEnteredRecords.size()>0) {
	    		for(JobRecord jobrecord: userEnteredRecords) {
	    			if(security.checkParameter(jobrecord.getGeonameID(), Parameter.LOCATION)){
		    			geonameIds.add(jobrecord.getGeonameID());
	    			}
	    		}
	    		log.info("geonameIds: "+geonameIds.size());
	    		Map<String, Location> geonamesMap;
				try {
					geonamesMap = hierarchyIndexSearcher.findGeonameLocations(geonameIds);
				} catch (LuceneSearcherException e) {
	    			log.warning("Geonames Lucene exception: " + e.getMessage());
	    			throw e;
				}
				Location loc = new Location();
		    	for(JobRecord jobrecord: userEnteredRecords) {
		    		if  (security.checkParameter(jobrecord.getId(), Parameter.RECORD_ID) && 
		    				security.checkParameter(jobrecord.getCollectionDate(), Parameter.DATE) &&
		    				security.checkParameter(jobrecord.getRawSequence(), Parameter.RAW_SEQUENCE)) {
		    			if (geonamesMap.containsKey(jobrecord.getGeonameID())){
		    				loc = geonamesMap.get(jobrecord.getGeonameID());
		    				loc.setAccession(jobrecord.getId());
		    			} else {
		    				loc = new Location();
		    			}
		    			FastaRecord fastaRecord = new FastaRecord(
		    					jobrecord.getId(), 
		    					jobrecord.getCollectionDate(), 
		    					jobrecord.getRawSequence(),
		    					loc);
		    			fastaRecords.add(fastaRecord);
		    			jobRecordIds.add(jobrecord.getId());
		    		}
		    		else {
		    			log.warning("Bad parameters for record : "+jobrecord.getId());
		    			throw new ParameterException(jobrecord.getId());
		    		}
		    	}
    		}
    		if (jobAccessions.size()+fastaRecords.size() > JOB_MAX_ACCESSIONS) {
    			log.warning("Record list is too long.");
    			throw new ParameterException("Record list is too long");
    		}
    		if (jobAccessions.size() > JOB_MAX_ACCESSIONS) {
    			log.warning("Job accession list is too long.");
    			throw new ParameterException("accessions list is too long");
    		}
    	    	if (fastaRecords.size() > JOB_MAX_ACCESSIONS) {
    	    		log.warning("FASTA record list is too long.");
    	    		throw new ParameterException("accessions list is too long");
    	    	}
    	    
    	    JobAccessions accessionsList = zoophy.testZooPhy(new ArrayList<String>(jobAccessions), fastaRecords, dao, hierarchyIndexSearcher);
	    	results.setAccessionsRemoved(accessionsList.getInvalidRecordList());
    	    results.setAccessionsUsed(new ArrayList<String>(accessionsList.getValidAccessions()));   	
	    	return results; 
	    	}
	    	else {
	    		log.warning("Bad reply email parameter: "+parameters.getReplyEmail());
	    		throw new ParameterException(parameters.getReplyEmail());
	    	}
    	}
    	catch (ParameterException pe) {
    		results.setError(pe.getMessage());
    		return results;
    	}
    	catch (GLMException glme) {
    		if (glme.getUserMessage() != null) {
    			results.setError(glme.getMessage());
        		return results;
    		}
    		else {
    			results.setError("GLM Tools Failed");
        		return results;
    		}
    	}
    	catch (PipelineException pe) {
    		if (pe.getUserMessage() != null) {
    			results.setError(pe.getUserMessage());
    			return results;
    			
    		}
    		else {
    			results.setError("ZooPhy Pipeline Failed");
    			return results;
    		}
    	}
    	catch (Exception e) {
    		log.warning("Unknown Pipeline error occurred: "+e.getMessage());;
    		results.setError("Unkown Error occurred");
    		return results;
    	}
    }
}
