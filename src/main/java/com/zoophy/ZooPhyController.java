package com.zoophy;

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

import com.zoophy.database.DaoException;
import com.zoophy.database.GenBankRecordNotFoundException;
import com.zoophy.database.ZoophyDAO;
import com.zoophy.genbank.GenBankRecord;
import com.zoophy.genbank.Location;
import com.zoophy.index.InvalidLuceneQueryException;
import com.zoophy.index.LuceneSearcher;
import com.zoophy.index.LuceneSearcherException;
import com.zoophy.pipeline.PipelineException;
import com.zoophy.pipeline.ZooPhyRunner;
import com.zoophy.security.Parameter;
import com.zoophy.security.ParameterException;
import com.zoophy.security.SecurityHelper;

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
     * @param query - Valid Lucene querystring
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
     * @param replyEmail
     * @param jobName
     * @param accessions
     * @throws ParameterException
     */
    @RequestMapping(value="/run", method=RequestMethod.POST)
    @ResponseStatus(value=HttpStatus.ACCEPTED)
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
	    	zoophy.runZooPhy(new ArrayList<String>(accs));
    	}
    	else {
    		throw new ParameterException(replyEmail);
    	}
    }
    
}
