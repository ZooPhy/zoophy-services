package com.zoophy.main;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zoophy.database.ZoophyDAO;
import com.zoophy.genbank.GenBankRecord;

@RestController
public class ZooPhyController {
	
	@Autowired
	ZoophyDAO dao;
    
    @RequestMapping("/record/")
    public GenBankRecord getRecord(@RequestParam(value="accession") String accession) {
    	GenBankRecord gbr = dao.retreiveRecord(accession);
    	return gbr;
    }
    
}
