package edu.asu.zoophy.rest.genbank;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GenBankRecordTest {

	@Test
	public void test() {
		GenBankRecord record = new GenBankRecord();
		assertNotNull(record);
		record.setAccession("ABC12345");
		Host host = new Host();
		host.setAccession("ABC12345");
		host.setName("Human");
		host.setTaxon(9606);
		record.setHost(host);
		Location loc = new Location();
		loc.setAccession("ABC12345");
		loc.setCountry("USA");
		loc.setGeonameID(5855739L);
		loc.setGeonameType("ADMD");
		loc.setLatitude(21.608);
		loc.setLocation("Home");
		loc.setLongitude(-157.917);
		record.setGeonameLocation(loc);
		Publication pub = new Publication();
		pub.setAuthors("demetri");
		pub.setCentralID("PMC808");
		pub.setJournal("MIT Technology Review");
		pub.setPubMedID(808);
		pub.setTitle("The Joy of Unit Testing");
		record.setPublication(pub);
		Sequence seq = new Sequence();
		seq.setAccession("ABC12345");
		seq.setCollectionDate("Feb1996");
		seq.setComment("Testing...");
		seq.setDefinition("Fake Sequence");
		seq.setIsolate("leon");
		seq.setOrganism("T Virus");
		seq.setPH1N1(false);
		seq.setRawSequence("gattica");
		seq.setSegmentLength(808);
		seq.setStrain("Raccoon City");
		seq.setTaxID(11908);
		record.setSequence(seq);
		Gene gene = new Gene();
		gene.setAccession("ABC12345");
		gene.setName("HA");
		List<Gene> genes = new LinkedList<Gene>();
		genes.add(gene);
		record.setGenes(genes);
		assertEquals("ABC12345", record.getAccession());
		assertNotNull(record.getGenes());
		assertEquals(1, record.getGenes().size());
		assertNotNull(record.getGeonameLocation());
		assertNotNull(record.getHost());
		assertNotNull(record.getPublication());
		assertNotNull(record.getSequence());
		assertEquals(gene, record.getGenes().get(0));
		assertEquals(host, record.getHost());
		assertEquals(pub, record.getPublication());
		assertEquals(seq, record.getSequence());
		assertEquals(loc, record.getGeonameLocation());
	}

}
