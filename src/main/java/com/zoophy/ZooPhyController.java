package com.zoophy;

import java.util.List;

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
import com.zoophy.index.InvalidLuceneQueryException;
import com.zoophy.index.LuceneSearcher;
import com.zoophy.index.LuceneSearcherException;

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
	
    /**
     * Retrieves the specified record from the database.
     * @param accession - Accession of GenBankRecord to be retrieved
     * @param isFull - does the record need its associated Publication and Genes
     * @return specified record from the database.
     * @throws GenBankRecordNotFoundException 
     * @throws DaoException 
     */
    @RequestMapping(value="/record", method=RequestMethod.GET)
    @ResponseStatus(value=HttpStatus.OK)
    public GenBankRecord getDatabaseRecord(@RequestParam(value="accession") String accession, @RequestParam(value="isfull", required=false, defaultValue="true") Boolean isFull) throws GenBankRecordNotFoundException, DaoException {
    	GenBankRecord gbr = null;
    	if (isFull) {
    		gbr = dao.retreiveFullRecord(accession);
    	}
    	else {
    		gbr = dao.retreiveLightRecord(accession);
    	}
    	return gbr;
    }
    
    /**
     * Retrieve GenBankRecords for resulting Lucene query
     * @param query - Valid Lucene querystring
     * @return GenBankRecord results of given query.
     * @throws LuceneSearcherException 
     * @throws InvalidLuceneQueryException 
     */
    @RequestMapping(value="/search", method=RequestMethod.GET)
    @ResponseStatus(value=HttpStatus.OK)
    public List<GenBankRecord> queryLucene(@RequestParam(value="query") String query) throws LuceneSearcherException, InvalidLuceneQueryException {
    	List<GenBankRecord> results = indexSearcher.searchIndex(query);
    	return results;
    }
    
    @RequestMapping(value="/run", method=RequestMethod.POST)
    @ResponseStatus(value=HttpStatus.ACCEPTED)
    public void runZooPhyJob(@RequestBody String jobID, @RequestBody String jobName, @RequestBody List<String> accessions) {
    	//TODO: run zoophy job
    }
    
}
