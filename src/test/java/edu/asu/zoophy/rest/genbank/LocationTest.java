package edu.asu.zoophy.rest.genbank;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class LocationTest {

	@Test
	public void test() {
		Location loc = new Location();
		assertNotNull(loc);
		loc.setAccession("ABC12345");
		loc.setCountry("USA");
		loc.setGeonameID(5855739L);
		loc.setGeonameType("ADMD");
		loc.setLatitude(21.608);
		loc.setLocation("Home");
		loc.setLongitude(-157.917);
		assertEquals("ABC12345", loc.getAccession());
		assertEquals("USA", loc.getCountry());
		assertEquals(5855739L, loc.getGeonameID().longValue());
		assertEquals("ADMD", loc.getGeonameType());
		assertEquals(21.608, loc.getLatitude().doubleValue(), 0.0);
		assertEquals("Home", loc.getLocation());
		assertEquals(-157.917, loc.getLongitude().doubleValue(), 0.0);
	}

}
