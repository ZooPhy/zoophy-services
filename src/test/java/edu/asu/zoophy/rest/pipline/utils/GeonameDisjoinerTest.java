package edu.asu.zoophy.rest.pipline.utils;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import edu.asu.zoophy.rest.genbank.GenBankRecord;
import edu.asu.zoophy.rest.genbank.JobRecords;
import edu.asu.zoophy.rest.genbank.Location;
import edu.asu.zoophy.rest.index.LuceneHierarchySearcher;
import edu.asu.zoophy.rest.pipeline.PipelineException;
import edu.asu.zoophy.rest.pipeline.utils.GeonameDisjoiner;

/**
 * Test cases for DisJoiner
 * @author kbhangal
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class GeonameDisjoinerTest {
	
	@Autowired
	private LuceneHierarchySearcher hierarchyIndexSearcher;
	
	/**
	 * Test the disjoiner for Single country and generic testing
	 * @return list of GenBankRecord objects
	 */
	@Test
	public void testDisjoiner() {
		try {
			GeonameDisjoiner disjoiner  = new GeonameDisjoiner(hierarchyIndexSearcher);
			JobRecords jobRecords = disjoiner.disjoinRecords(testUSParameter());
			assertEquals(6, jobRecords.getDistinctLocations().intValue());
		} catch (PipelineException e) {
			fail("Should not throw Pipeline Error");
		}
	}
	
	/**
	 * Test disJoiner for too Many locations 
	 * @return list of GenBankRecord objects
	 */
	@Test
	public void testTooManyDisjoiner() {
		try {
			GeonameDisjoiner disjoiner  = new GeonameDisjoiner(hierarchyIndexSearcher);
			disjoiner.disjoinRecords(tooManyParameter());
		} catch (PipelineException e) {
			assertEquals("Too many distinct locations (limit is 25): 26\n" + 
					"Locations: China, Italy, Czechia, Germany, India, Japan, Mexico, "
					+ "Myanmar, Russia, Spain, Sweden, Thailand, Hawaii, Arizona, New York, "
					+ "Wisconsin, District of Columbia, Oregon, Georgia, Idaho, Alaska, Utah, "
					+ "Saskatchewan, Québec, Ontario, Alberta", e.getUserMessage());
		}
	}
	
	/**
	 * Test disJoiner for too few locations 
	 * @return list of GenBankRecord objects
	 */
	@Test
	public void testTooFewDisjoiner() {
		try {
			GeonameDisjoiner disjoiner  = new GeonameDisjoiner(hierarchyIndexSearcher);
			disjoiner.disjoinRecords(tooFewParameter());
		} catch (PipelineException e) {
			assertEquals("Too few distinct locations (need at least 2): 1\n" + 
					"Location: New York", e.getUserMessage());
		}
	}
	
	/*
	 * 25 countries with 3 US locations
	 * DisJoiner should pass with 25 country level locations
	 * Previous disJoiner failed since it couldn't reduce 3 US locations at ADM1 to 1 location at PCLI
	 */
	@Test
	public void sampleDisjoiner1() {
		try {
			GeonameDisjoiner disjoiner  = new GeonameDisjoiner(hierarchyIndexSearcher);
			JobRecords jobRecords = disjoiner.disjoinRecords(specialCase1());
		assertEquals(25, jobRecords.getDistinctLocations().intValue());
		} catch (PipelineException e) {
			fail("Should not throw Pipeline Error : "+ e.getUserMessage());
		}
	}
	
	/*
	 * generic test 
	 */
	@Test
	public void sampleDisjoiner2() {
		try {
			GeonameDisjoiner disjoiner  = new GeonameDisjoiner(hierarchyIndexSearcher);
			disjoiner.disjoinRecords(specialCase2());
		} catch (PipelineException e) {
			assertEquals("Too few distinct locations (need at least 2): 1\n" + 
					"Location: New York", e.getUserMessage());
		}
	}
	
	private List<GenBankRecord> testUSParameter() {
		/* United States - Maryland(ADM1),California(ADM1),Nevada(ADM1),Utah(ADM1),
		 		Minnesota(ADM1),New York(ADM1),New York(ADM1),United states(PCLI) */
		
		List<GenBankRecord> recordsList = new LinkedList<>();
		
		GenBankRecord record = new GenBankRecord();
		record.setAccession("AB12345");
		Location location = new Location();
		location.setGeonameID((long) 4361885);
		location.setAccession("AB12345");
		location.setLocation("Maryland");
		location.setGeonameType("ADM1");
		location.setCountry("United States");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		record.setAccession("AB12346");
		location = new Location();
		location.setGeonameID((long) 5332921);
		location.setAccession("AB12346");
		location.setLocation("California");
		location.setGeonameType("ADM1");
		location.setCountry("United States");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		record.setAccession("AB12347");
		location = new Location();
		location.setGeonameID((long) 5509151);
		location.setAccession("AB12347");
		location.setLocation("Nevada");
		location.setGeonameType("ADM1");
		location.setCountry("United States");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		record.setAccession("AB12348");
		location = new Location();
		location.setGeonameID((long) 5549030);
		location.setAccession("AB12348");
		location.setLocation("Utah");
		location.setGeonameType("ADM1");
		location.setCountry("United States");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		record.setAccession("AB12349");
		location = new Location();
		location.setGeonameID((long) 5037779);
		location.setAccession("AB12349");
		location.setLocation("Minnesota");
		location.setGeonameType("ADM1");
		location.setCountry("United States");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		record.setAccession("AB12310");
		location = new Location();
		location.setGeonameID((long) 5128581);
		location.setAccession("AB12310");
		location.setLocation("New York");
		location.setGeonameType("ADM1");
		location.setCountry("United States");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		record.setAccession("AB12311");
		location = new Location();
		location.setGeonameID((long) 5128581);
		location.setAccession("AB12311");
		location.setLocation("New York");
		location.setGeonameType("ADM1");
		location.setCountry("United States");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		record.setAccession("AB12312");
		location = new Location();
		location.setGeonameID((long) 5128581);
		location.setAccession("AB12312");
		location.setLocation("United States");
		location.setGeonameType("PCLI");
		location.setCountry("United States");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		return recordsList;
	}
	
	private List<GenBankRecord> tooFewParameter() {
		/* United States - New York(ADM1),New York(ADM1) */
		List<GenBankRecord> recordsList = new LinkedList<>();
		
		GenBankRecord record = new GenBankRecord();
		record.setAccession("AB12310");
		Location location = new Location();
		location.setGeonameID((long) 5128581);
		location.setAccession("AB12310");
		location.setLocation("New York");
		location.setGeonameType("ADM1");
		location.setCountry("United States");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		record.setAccession("AB12311");
		location = new Location();
		location.setGeonameID((long) 5128581);
		location.setAccession("AB12311");
		location.setLocation("New York");
		location.setGeonameType("ADM1");
		location.setCountry("United States");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		return recordsList;
	}
	
	private List<GenBankRecord> tooManyParameter() {
		/* 	United States - Hawaii(ADM1),Hawaii(ADM1),Hawaii(ADM1),Arizona(ADM1),
		 		New York(ADM1),Wisconsin(ADM1),District of Columbia(ADM1),Oregon(ADM1),
		 		Georgia(ADM1),Idaho(ADM1),Alaska(ADM1),Utah(ADM1)
		   	Canada - Saskatchewan(ADM1),Saskatchewan(ADM1),Québec(ADM1),Québec(ADM1),
		   		Ontario(ADM1),Alberta(ADM1)
		   	China - Zhuhai(PPLA2),Chengdu(PPLA),Jiangyin(PPLA4),Guangdong Sheng(ADM1),
		   		Shandong Sheng(ADM1)
		   	Italy-  Repubblica Italiana(PCLI),Pavia(ADM3),Ancona(ADM3)
		   	Russia-  Vladivostok(PPLA)
			Japan - Hiroshima-ken(ADM1)
			India - Pune(PPL)
			Thailand - Kingdom of Thailand(PCLI)
			Czechia-  Prague(PPLC)
			Germany - Free and Hanseatic City of Hamburg(ADM1)
			Mexico - Mexico(PCLI)
			Myanmar - Union of Burma(PCLI)
			Spain-  Catalunya(ADM1)
			Sweden - Skåne län(ADM1) */
		List<GenBankRecord> recordsList = new LinkedList<>();
		
		GenBankRecord record = new GenBankRecord();
		String accession = "GQ466001";
		record.setAccession(accession);
		Location location = new Location();
		location.setGeonameID((long) 5855797);
		location.setAccession(accession);
		location.setLocation("Hawaii");
		location.setGeonameType("ADM1");
		location.setCountry("United States");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466002";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 5855797);
		location.setAccession(accession);
		location.setLocation("Hawaii");
		location.setGeonameType("ADM1");
		location.setCountry("United States");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466003";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 5855797);
		location.setAccession(accession);
		location.setLocation("Hawaii");
		location.setGeonameType("ADM1");
		location.setCountry("United States");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466004";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 5551752);
		location.setAccession(accession);
		location.setLocation("Arizona");
		location.setGeonameType("ADM1");
		location.setCountry("United States");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466005";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 5128581);
		location.setAccession(accession);
		location.setLocation("New York");
		location.setGeonameType("ADM1");
		location.setCountry("United States");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466006";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 5279468);
		location.setAccession(accession);
		location.setLocation("Wisconsin");
		location.setGeonameType("ADM1");
		location.setCountry("United States");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466007";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 4138106);
		location.setAccession(accession);
		location.setLocation("District of Columbia");
		location.setGeonameType("ADM1");
		location.setCountry("United States");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466008";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 5744337);
		location.setAccession(accession);
		location.setLocation("Oregon");
		location.setGeonameType("ADM1");
		location.setCountry("United States");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466009";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 4197000);
		location.setAccession(accession);
		location.setLocation("Georgia");
		location.setGeonameType("ADM1");
		location.setCountry("United States");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466010";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 5596512);
		location.setAccession(accession);
		location.setLocation("Idaho");
		location.setGeonameType("ADM1");
		location.setCountry("United States");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466011";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 5879092);
		location.setAccession(accession);
		location.setLocation("Alaska");
		location.setGeonameType("ADM1");
		location.setCountry("United States");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466012";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 5549030);
		location.setAccession(accession);
		location.setLocation("Utah");
		location.setGeonameType("ADM1");
		location.setCountry("United States");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466013";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 6141242);
		location.setAccession(accession);
		location.setLocation("Saskatchewan");
		location.setGeonameType("ADM1");
		location.setCountry("Canada");
		record.setGeonameLocation(location);
		recordsList.add(record);

		record = new GenBankRecord();
		accession = "GQ466014";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 6141242);
		location.setAccession(accession);
		location.setLocation("Saskatchewan");
		location.setGeonameType("ADM1");
		location.setCountry("Canada");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466015";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 6115047);
		location.setAccession(accession);
		location.setLocation("Québec");
		location.setGeonameType("ADM1");
		location.setCountry("Canada");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466016";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 6115047);
		location.setAccession(accession);
		location.setLocation("Québec");
		location.setGeonameType("ADM1");
		location.setCountry("Canada");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466017";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 6093943);
		location.setAccession(accession);
		location.setLocation("Ontario");
		location.setGeonameType("ADM1");
		location.setCountry("Canada");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466018";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 5883102);
		location.setAccession(accession);
		location.setLocation("Alberta");
		location.setGeonameType("ADM1");
		location.setCountry("Canada");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466019";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 1790437);
		location.setAccession(accession);
		location.setLocation("Zhuhai");
		location.setGeonameType("PPLA2");
		location.setCountry("China");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466020";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 1815286);
		location.setAccession(accession);
		location.setLocation("Chengdu");
		location.setGeonameType("PPLA");
		location.setCountry("China");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466021";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 1806213);
		location.setAccession(accession);
		location.setLocation("Jiangyin");
		location.setGeonameType("PPLA4");
		location.setCountry("China");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466022";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 1809935);
		location.setAccession(accession);
		location.setLocation("Guangdong Sheng");
		location.setGeonameType("ADM1");
		location.setCountry("China");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466023";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 1796328);
		location.setAccession(accession);
		location.setLocation("Shandong Sheng");
		location.setGeonameType("ADM1");
		location.setCountry("China");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466024";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 3175395);
		location.setAccession(accession);
		location.setLocation("Repubblica Italiana");
		location.setGeonameType("PCLI");
		location.setCountry("Italy");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466025";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 6541854);
		location.setAccession(accession);
		location.setLocation("Pavia");
		location.setGeonameType("ADM3");
		location.setCountry("Italy");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466026";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 6542126);
		location.setAccession(accession);
		location.setLocation("Ancona");
		location.setGeonameType("ADM3");
		location.setCountry("Italy");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466027";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 2013348);
		location.setAccession(accession);
		location.setLocation("Vladivostok");
		location.setGeonameType("PPLA");
		location.setCountry("Russia");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466028";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 1862413);
		location.setAccession(accession);
		location.setLocation("Hiroshima-ken");
		location.setGeonameType("ADM1");
		location.setCountry("Japan");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466029";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 1259229);
		location.setAccession(accession);
		location.setLocation("Pune");
		location.setGeonameType("PPL");
		location.setCountry("India");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466030";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 1605651);
		location.setAccession(accession);
		location.setLocation("Kingdom of Thailand");
		location.setGeonameType("PCLI");
		location.setCountry("Thailand");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466031";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 3067696);
		location.setAccession(accession);
		location.setLocation("Prague");
		location.setGeonameType("PPLC");
		location.setCountry("Czechia");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466032";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 2911297);
		location.setAccession(accession);
		location.setLocation("Free and Hanseatic City of Hamburg");
		location.setGeonameType("ADM1");
		location.setCountry("Germany");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466033";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 3996063);
		location.setAccession(accession);
		location.setLocation("Mexico");
		location.setGeonameType("PCLI");
		location.setCountry("Mexico");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466034";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 1327865);
		location.setAccession(accession);
		location.setLocation("Union of Burma");
		location.setGeonameType("PCLI");
		location.setCountry("Myanmar");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466035";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 3336901);
		location.setAccession(accession);
		location.setLocation("Catalunya");
		location.setGeonameType("ADM1");
		location.setCountry("Spain");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ466036";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 3337385);
		location.setAccession(accession);
		location.setLocation("Skåne län");
		location.setGeonameType("ADM1");
		location.setCountry("Sweden");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		return recordsList;
	}
	
	private List<GenBankRecord> specialCase2() {
		List<GenBankRecord> recordsList = new LinkedList<>();
		
		GenBankRecord record = new GenBankRecord();
		String accession = "";
		record.setAccession("AB12312");
		Location location = new Location();
		location.setGeonameID((long) 5128581);
		location.setAccession("AB12312");
		location.setLocation("United States");
		location.setGeonameType("PCLI");
		location.setCountry("United States");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		record.setAccession("GQ475636");
		location = new Location();
		location.setGeonameID((long) 5551752);
		location.setAccession("GQ475636");
		location.setLocation("Arizona");
		location.setGeonameType("ADM1");
		location.setCountry("United States");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		record.setAccession("GQ475000");
		location = new Location();
		location.setGeonameID((long) 5308655);
		location.setAccession("GQ475000");
		location.setLocation("Phoenix");
		location.setGeonameType("PPLA");
		location.setCountry("United States");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		record.setAccession("GQ475969");
		location = new Location();
		location.setGeonameID((long) 5128581);
		location.setAccession("GQ475969");
		location.setLocation("New York");
		location.setGeonameType("ADM1");
		location.setCountry("United States");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ464408";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 3336901);
		location.setAccession(accession);
		location.setLocation("Catalunya");
		location.setGeonameType("ADM1");
		location.setCountry("Spain");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GU236519";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 3337385);
		location.setAccession(accession);
		location.setLocation("Skåne län");
		location.setGeonameType("ADM1");
		location.setCountry("Sweden");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		return recordsList;
	}
	
	private List<GenBankRecord> specialCase1() {
		List<GenBankRecord> recordsList = new LinkedList<>();
		
		GenBankRecord record = new GenBankRecord();
		String accession = "";
		record.setAccession("GQ466385");
		Location location = new Location();
		location.setGeonameID((long) 5855797);
		location.setAccession("GQ466385");
		location.setLocation("Hawaii");
		location.setGeonameType("ADM1");
		location.setCountry("United States");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		record.setAccession("GQ475636");
		location = new Location();
		location.setGeonameID((long) 5551752);
		location.setAccession("GQ475636");
		location.setLocation("Arizona");
		location.setGeonameType("ADM1");
		location.setCountry("United States");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		record.setAccession("GQ475969");
		location = new Location();
		location.setGeonameID((long) 5128581);
		location.setAccession("GQ475969");
		location.setLocation("New York");
		location.setGeonameType("ADM1");
		location.setCountry("United States");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ457549";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 6141242);
		location.setAccession(accession);
		location.setLocation("Saskatchewan");
		location.setGeonameType("ADM1");
		location.setCountry("Canada");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ497077";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 1790437);
		location.setAccession(accession);
		location.setLocation("Zhuhai");
		location.setGeonameType("PPLA2");
		location.setCountry("China");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GU123924";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 3175395);
		location.setAccession(accession);
		location.setLocation("Repubblica Italiana");
		location.setGeonameType("PCLI");
		location.setCountry("Italy");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ496142";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 2013348);
		location.setAccession(accession);
		location.setLocation("Vladivostok");
		location.setGeonameType("PPLA");
		location.setCountry("Russia");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GU014785";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 1862413);
		location.setAccession(accession);
		location.setLocation("Hiroshima-ken");
		location.setGeonameType("ADM1");
		location.setCountry("Japan");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GU292361";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 1259229);
		location.setAccession(accession);
		location.setLocation("Pune");
		location.setGeonameType("PPL");
		location.setCountry("India");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ866924";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 1605651);
		location.setAccession(accession);
		location.setLocation("Kingdom of Thailand");
		location.setGeonameType("PCLI");
		location.setCountry("Thailand");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GU290049";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 3067696);
		location.setAccession(accession);
		location.setLocation("Prague");
		location.setGeonameType("PPLC");
		location.setCountry("Czechia");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GU480807";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 2911297);
		location.setAccession(accession);
		location.setLocation("Free and Hanseatic City of Hamburg");
		location.setGeonameType("ADM1");
		location.setCountry("Germany");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ463205";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 3996063);
		location.setAccession(accession);
		location.setLocation("Mexico");
		location.setGeonameType("PCLI");
		location.setCountry("Mexico");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GU014796";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 1327865);
		location.setAccession(accession);
		location.setLocation("Union of Burma");
		location.setGeonameType("PCLI");
		location.setCountry("Myanmar");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ464408";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 3336901);
		location.setAccession(accession);
		location.setLocation("Catalunya");
		location.setGeonameType("ADM1");
		location.setCountry("Spain");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GU236519";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 3337385);
		location.setAccession(accession);
		location.setLocation("Skåne län");
		location.setGeonameType("ADM1");
		location.setCountry("Sweden");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "AJ457887";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 2253350);
		location.setAccession(accession);
		location.setLocation("Dhaka");
		location.setGeonameType("ADM1");
		location.setCountry("Senegal");
		record.setGeonameLocation(location);
		recordsList.add(record);

		record = new GenBankRecord();
		accession = "AJ457906";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 993800);
		location.setAccession(accession);
		location.setLocation("Gauteng");
		location.setGeonameType("ADM1");
		location.setCountry("South Africa");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "CY071811";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 2460594);
		location.setAccession(accession);
		location.setLocation("Bamako");
		location.setGeonameType("ADM1");
		location.setCountry("Mali");
		record.setGeonameLocation(location);
		recordsList.add(record);

		record = new GenBankRecord();
		accession = "CY071816";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 344979);
		location.setAccession(accession);
		location.setLocation("Addis Ababa");
		location.setGeonameType("ADM1");
		location.setCountry("Ethiopia");
		record.setGeonameLocation(location);
		recordsList.add(record);

		record = new GenBankRecord();
		accession = "CY073156";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 2332453);
		location.setAccession(accession);
		location.setLocation("Lagos");
		location.setGeonameType("ADM1");
		location.setCountry("Nigeria");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "CY080589";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 2464645);
		location.setAccession(accession);
		location.setLocation("Tawzar");
		location.setGeonameType("ADM1");
		location.setCountry("Tunisia");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "DQ534415";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 6546897);
		location.setAccession(accession);
		location.setLocation("Rabat");
		location.setGeonameType("ADM1");
		location.setCountry("Morocco");
		record.setGeonameLocation(location);
		recordsList.add(record);

		record = new GenBankRecord();
		accession = "EU770350";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 187125);
		location.setAccession(accession);
		location.setLocation("Nairobi Area");
		location.setGeonameType("ADM1");
		location.setCountry("Kenya");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "GQ422376";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 1106827);
		location.setAccession(accession);
		location.setLocation("Plaines Wilhems");
		location.setGeonameType("ADM1");
		location.setCountry("Mauritius");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		record = new GenBankRecord();
		accession = "JF707781";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 2223603);
		location.setAccession(accession);
		location.setLocation("North");
		location.setGeonameType("ADM1");
		location.setCountry("Cameroon");
		record.setGeonameLocation(location);
		recordsList.add(record);

		record = new GenBankRecord();
		accession = "KJ026428";
		record.setAccession(accession);
		location = new Location();
		location.setGeonameID((long) 7732554);
		location.setAccession(accession);
		location.setLocation("Abidjan");
		location.setGeonameType("ADM1");
		location.setCountry("Ivory Coast");
		record.setGeonameLocation(location);
		recordsList.add(record);
		
		return recordsList;
	}
}
