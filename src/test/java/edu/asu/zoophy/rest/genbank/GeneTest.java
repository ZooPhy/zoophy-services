package edu.asu.zoophy.rest.genbank;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GeneTest {

	@Test
	public void test() {
		Gene gene = new Gene();
		assertNotNull(gene);
		gene.setAccession("ABC12345");
		gene.setName("HA");
		assertEquals("ABC12345", gene.getAccession());
		assertEquals("HA", gene.getName());
	}

}
