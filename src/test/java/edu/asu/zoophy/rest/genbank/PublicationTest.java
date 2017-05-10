package edu.asu.zoophy.rest.genbank;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PublicationTest {

	@Test
	public void test() {
		Publication pub = new Publication();
		assertNotNull(pub);
		pub.setAuthors("demetri");
		pub.setCentralID("PMC808");
		pub.setJournal("MIT Technology Review");
		pub.setPubMedID(808);
		pub.setTitle("The Joy of Unit Testing");
		assertEquals("demetri", pub.getAuthors());
		assertEquals("PMC808", pub.getCentralID());
		assertEquals("MIT Technology Review", pub.getJournal());
		assertEquals(808, pub.getPubMedID().intValue());
		assertEquals("The Joy of Unit Testing", pub.getTitle());
	}

}
