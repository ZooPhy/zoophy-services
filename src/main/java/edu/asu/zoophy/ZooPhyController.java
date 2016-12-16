package edu.asu.zoophy;

import java.util.ArrayList;
import java.util.LinkedHashSet;
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
import edu.asu.zoophy.pipeline.ZooPhyRunner;
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
	
	@Value("${job.max.accessions}")
	private Integer JOB_MAX_ACCESSIONS;
	
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
     * @param replyEmail - User email for results
     * @param jobName - Custom job name (optional)
     * @param accessions - List of accessions to to run the job on
     * @throws ParameterException
     */
    @RequestMapping(value="/run", method=RequestMethod.POST, headers="Accept=application/json")
    @ResponseStatus(value=HttpStatus.ACCEPTED)
    public void runZooPhyJob(@RequestBody JobParameters parameters) throws ParameterException, PipelineException {
    	if (security.checkParameter(parameters.getReplyEmail(), Parameter.EMAIL)) {
    		ZooPhyRunner zoophy;
    		//TODO: fix NullPointerExceptions 
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
	    	zoophy.runZooPhy(new ArrayList<String>(jobAccessions));
    	}
    	else {
    		throw new ParameterException(parameters.getReplyEmail());
    	}
    }
    
}
