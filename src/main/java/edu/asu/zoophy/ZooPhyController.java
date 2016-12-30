package edu.asu.zoophy;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import edu.asu.zoophy.database.DaoException;
import edu.asu.zoophy.database.GenBankRecordNotFoundException;
import edu.asu.zoophy.database.ZoophyDAO;
import edu.asu.zoophy.genbank.GenBankRecord;
import edu.asu.zoophy.genbank.Location;
import edu.asu.zoophy.index.InvalidLuceneQueryException;
import edu.asu.zoophy.index.LuceneSearcher;
import edu.asu.zoophy.index.LuceneSearcherException;
import edu.asu.zoophy.pipeline.PipelineException;
import edu.asu.zoophy.pipeline.PipelineManager;
import edu.asu.zoophy.pipeline.ZooPhyRunner;
import edu.asu.zoophy.pipeline.utils.DownloadFormatter;
import edu.asu.zoophy.security.Parameter;
import edu.asu.zoophy.security.ParameterException;
import edu.asu.zoophy.security.SecurityHelper;

/**
 * Responsible for mapping ZooPhy service requests
 * @author devdemetri
 */
@RestController
public class ZooPhyController {
	
	@Autowired
	private ZoophyDAO dao;
	
	@Autowired
	private LuceneSearcher indexSearcher;
	
	@Autowired
	private SecurityHelper security;
	
	@Autowired
	private PipelineManager manager;
	
	@Value("${job.max.accessions}")
	private Integer JOB_MAX_ACCESSIONS;
	
	@Autowired
	private DownloadFormatter formatter;
	
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
	    		record = dao.retrieveFullRecord(accession);
	    	}
	    	else {
	    		record = dao.retrieveLightRecord(accession);
	    	}
	    	return record;
    	}
    	else {
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
    		Location location = dao.retrieveLocation(accession);
    		return location;
    	}
    	else {
    		throw new ParameterException(accession);
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
    		List<GenBankRecord> results = indexSearcher.searchIndex(query);
    		return results;
    	}
    	else {
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
    	if (accessions != null && accessions.size() > 0 && accessions.size() < 1023) {
    		Set<String> uniqueAccessions = new LinkedHashSet<String>(accessions.size());
    		for (String accession : accessions) {
    			if (security.checkParameter(accession, Parameter.ACCESSION)) {
    				uniqueAccessions.add(accession);
    			}
    			else {
    				throw new ParameterException(accession);
    			}
    		}
    		List<String> usableAccessions = new LinkedList<String>(uniqueAccessions);
    		uniqueAccessions.clear();
    		StringBuilder queryBuilder = new StringBuilder("Accession: (");
    		queryBuilder.append(usableAccessions.get(0));
    		usableAccessions.remove(0);
    		for (String accession : usableAccessions) {
    			queryBuilder.append(" OR ");
    			queryBuilder.append(accession);
    		}
    		usableAccessions.clear();
    		queryBuilder.append(")");
    		// TODO: Need to use a different method to allow over 1024 accessions down the road. 
    		records = indexSearcher.searchIndex(queryBuilder.toString());
    	}
    	return records;
    }
    
    /**
     * @param replyEmail - User email for results
     * @param jobName - Custom job name (optional)
     * @param accessions - List of accessions to to run the job on
     * @return JobID for the started ZooPhy job
     * @throws ParameterException
     */
    @RequestMapping(value="/run", method=RequestMethod.POST, headers="Accept=application/json")
    @ResponseStatus(value=HttpStatus.ACCEPTED)
    public String runZooPhyJob(@RequestBody JobParameters parameters) throws ParameterException, PipelineException {
    	if (security.checkParameter(parameters.getReplyEmail(), Parameter.EMAIL)) {
    		ZooPhyRunner zoophy;
	    	if (parameters.getJobName() == null) {
	    			zoophy = new ZooPhyRunner(parameters.getReplyEmail(), null);
	    	}
	    	else {
	    		if (security.checkParameter(parameters.getJobName(), Parameter.JOB_NAME)) {
	    			zoophy = new ZooPhyRunner(parameters.getReplyEmail(), parameters.getJobName());
	    		}
	    		else {
	    			throw new ParameterException(parameters.getJobName());
	    		}
	    	}
	    	Set<String> jobAccessions = new LinkedHashSet<String>(parameters.getAccessions().size());
	    	for(String accession : parameters.getAccessions()) {
	    		if  (security.checkParameter(accession, Parameter.ACCESSION)) {
	    			jobAccessions.add(accession);
	    		}
	    		else {
	    			throw new ParameterException(accession);
	    		}
	    	}
	    	if (jobAccessions.size() > JOB_MAX_ACCESSIONS) {
	    		throw new ParameterException("accessions list is too long");
	    	}
	    	manager.startZooPhyPipeline(zoophy, new ArrayList<String>(jobAccessions));
	    	return zoophy.getJobID();
    	}
    	else {
    		throw new ParameterException(parameters.getReplyEmail());
    	}
    }
    
    /**
     * Stop a running ZooPhyJob by the Job ID
     * @param jobID - ID of Job to be stopped
     * @throws PipelineException
     * @throws ParameterException
     */
    @RequestMapping(value="/stop", method=RequestMethod.GET)
    @ResponseStatus(value=HttpStatus.OK)
    public String stopZooPhyJob(@RequestParam(value="id") String jobID) throws PipelineException, ParameterException {
    	if (security.checkParameter(jobID, Parameter.JOB_ID)) {
    		try {
	    		manager.killJob(jobID);
	    		return "Successfully stopped ZooPhy Job: "+jobID;
    		}
    		catch (PipelineException pe) {
    			throw new ParameterException(jobID);
    		}
    	}
    	else {
    		throw new ParameterException(jobID);
    	}
    }
    
    
    
}
