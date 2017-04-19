package edu.asu.zoophy.rest.index;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import edu.asu.zoophy.rest.genbank.GenBankRecord;

@RunWith(SpringRunner.class)
@SpringBootTest
public class LuceneSearcherTest {

	@Autowired
	LuceneSearcher searcher;
	
	@Test
	public void testGetRecord() {
		assertNotNull(searcher);
		try {
			GenBankRecord record = searcher.getRecord("CY187660");
			assertNotNull(record);
			assertEquals("CY187660", record.getAccession());
			assertEquals("United States", record.getGeonameLocation().getCountry());
			assertEquals("HA", record.getGenes().get(0).getName());
			assertEquals("human; gender m; age 25", record.getHost().getName());
			assertEquals(true, record.getSequence().getIsPH1N1());
			assertEquals(1701, record.getSequence().getSegmentLength().intValue());
		}
		catch (LuceneSearcherException lse) {
			fail("Should not throw Lucene Error");
		}
	}
	
	@Test
	public void testFindLocationAncestors() {
		fail("TODO");
	}
	
	@Test void testSearchIndex() {
		fail("TODO");
	}
	
	@Test void testTestIndex() {
		fail("TODO");
	}

}
