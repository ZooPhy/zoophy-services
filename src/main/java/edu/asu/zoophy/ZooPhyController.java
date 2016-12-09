package edu.asu.zoophy;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
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
	    	GenBankRecord gbr = null;
	    	if (isFull) {
	    		gbr = dao.retrieveFullRecord(accession);
	    	}
	    	else {
	    		gbr = dao.retrieveLightRecord(accession);
	    	}
	    	return gbr;
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
    		Location loc = dao.retrieveLocation(accession);
    		return loc;
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
     * Finds all of the parent locations for a specific record.
     * Note: this will be removed when the pipeline is integrated into these services.
     * @param accession - accession to check
     * @return Set of Geoname IDs that are parents of the given record's location
     * @throws LuceneSearcherException 
     * @throws ParameterException 
     * @throws DaoException 
     * @throws GenBankRecordNotFoundException 
     */
    @RequestMapping(value="/location/ancestors", method=RequestMethod.GET)
    @ResponseStatus(value=HttpStatus.OK)
    public Set<Long> getRecordLocationAncestors(@RequestParam(value="accession") String accession) throws LuceneSearcherException, ParameterException, GenBankRecordNotFoundException, DaoException {
    	if (security.checkParameter(accession, Parameter.ACCESSION)) {
    		Location loc = dao.retrieveLocation(accession);
    		Set<Long> ancestors = indexSearcher.findLocationAncestors(accession);
    		ancestors.remove(loc.getGeonameID());
    		return ancestors;
    	}
    	else {
    		throw new ParameterException(accession);
    	}
    }
    
    /**
     * @param replyEmail - User email for results
     * @param jobName - Custom job name (optional)
     * @param accessions - List of accessions to to run the job on
     * @throws ParameterException
     */
    @RequestMapping(value="/run", method=RequestMethod.POST, headers="Accept=application/json")
    @ResponseStatus(value=HttpStatus.NOT_IMPLEMENTED)//TODO: finish pipeline implementation
    public void runZooPhyJob(@RequestBody String replyEmail, @RequestBody(required=false) String jobName, @RequestBody List<String> accessions) throws ParameterException, PipelineException {
    	if (security.checkParameter(replyEmail, Parameter.EMAIL)) {
    		ZooPhyRunner zoophy;
	    	if (jobName == null) {
	    			zoophy = new ZooPhyRunner(replyEmail);
	    	}
	    	else {
	    		if (security.checkParameter(jobName, Parameter.JOB_NAME)) {
	    			zoophy = new ZooPhyRunner(replyEmail, jobName);
	    		}
	    		else {
	    			throw new ParameterException(jobName);
	    		}
	    	}
	    	Set<String> accs = new LinkedHashSet<String>(accessions.size());
	    	for(String acc : accessions) {
	    		if  (security.checkParameter(acc, Parameter.ACCESSION)) {
	    			accs.add(acc);
	    		}
	    		else {
	    			throw new ParameterException(acc);
	    		}
	    	}
	    	if (accs.size() > 500) {
	    		throw new ParameterException("accessions list is too long");
	    	}
	    	zoophy.runZooPhy(new ArrayList<String>(accs));
    	}
    	else {
    		throw new ParameterException(replyEmail);
    	}
    }
    
}
