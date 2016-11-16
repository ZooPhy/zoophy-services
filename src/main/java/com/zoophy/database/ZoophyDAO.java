package com.zoophy.database;


import java.util.LinkedList;
import java.util.List;

import com.zoophy.genbank.GenBankRecord;
import com.zoophy.genbank.Gene;
import com.zoophy.genbank.Host;
import com.zoophy.genbank.Location;
import com.zoophy.genbank.Publication;
import com.zoophy.genbank.Sequence;

/**
 * Responsible for retreiving data from the SQL database.
 * @author devdemetri
 */
public class ZoophyDAO {
	
	public GenBankRecord retreiveRecord(String accession) {
		//TODO: actually retreive from database
		GenBankRecord rec = new GenBankRecord();
		rec.setAccession(accession);
		Host host = new Host();
		host.setAccession(accession);
		host.setName("Human");
		host.setTaxon(9606);
		rec.setHost(host);
		Location gbLoc = new Location();
		gbLoc.setAccession(accession);
		gbLoc.setId(1L);
		gbLoc.setLocation("Somewhere");
		gbLoc.setLatitude(404.4);
		gbLoc.setLongitude(808.8);
		gbLoc.setGeonameType("PCLI");
		rec.setGeonameLocation(gbLoc);
		Sequence seq = new Sequence();
		seq.setAccession(accession);
		seq.setCollection_date("11-08-2016");
		seq.setDefinition("Fake record");
		seq.setStrain("H40N4");
		seq.setComment("derp");
		seq.setIsolate("Stuff");
		seq.setOrganism("Virus");
		seq.setSegment_length(808);
		seq.setTax_id(1234);
		Publication pub = new Publication();
		pub.setPubId(404);
		pub.setCentralId("808");
		pub.setJournal("EST");
		pub.setAuthors("DevDemetri");
		pub.setTitle("Bad Viruses");
		seq.setPub(pub);
		rec.setSequence(seq);
		List<Gene> genes = new LinkedList<Gene>();
		Gene gen = new Gene();
		gen.setAccession(accession);
		gen.setId(808L);
		gen.setName("NS");
		genes.add(gen);
		rec.setGenes(genes);
		return rec;
	}
	
}
