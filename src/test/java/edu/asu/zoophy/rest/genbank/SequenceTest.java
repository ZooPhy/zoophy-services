package edu.asu.zoophy.rest.genbank;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SequenceTest {

	@Test
	public void test() {
		Sequence seq = new Sequence();
		assertNotNull(seq);
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
		assertEquals("ABC12345", seq.getAccession());
		assertEquals("Feb1996", seq.getCollectionDate());
		assertEquals("Testing...", seq.getComment());
		assertEquals("Fake Sequence", seq.getDefinition());
		assertEquals("leon", seq.getIsolate());
		assertEquals("T Virus", seq.getOrganism());
		assertEquals(false, seq.getIsPH1N1().booleanValue());
		assertEquals("gattica", seq.getRawSequence());
		assertEquals(808, seq.getSegmentLength().intValue());
		assertEquals("Raccoon City", seq.getStrain());
		assertEquals(11908, seq.getTaxID().intValue());
	}

}
