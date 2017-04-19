package edu.asu.zoophy.rest.index;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class LuceneSearcherExceptionTest {

	@Test
	public void test() {
		LuceneSearcherException lse = new LuceneSearcherException("Lucene ERRROR!");
		assertNotNull(lse);
		assertEquals("Lucene ERRROR!", lse.getMessage());
	}

}
