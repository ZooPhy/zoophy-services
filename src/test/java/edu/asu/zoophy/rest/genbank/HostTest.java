package edu.asu.zoophy.rest.genbank;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class HostTest {

	@Test
	public void test() {
		Host host = new Host();
		assertNotNull(host);
		host.setAccession("ABC12345");
		host.setName("Human");
		host.setTaxon(9606);
		assertEquals("ABC12345", host.getAccession());
		assertEquals("Human", host.getName());
		assertEquals(9606, host.getTaxon().intValue());
	}

}
