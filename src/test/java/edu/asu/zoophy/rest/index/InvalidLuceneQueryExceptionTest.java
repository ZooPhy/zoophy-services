package edu.asu.zoophy.rest.index;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class InvalidLuceneQueryExceptionTest {

	@Test
	public void test() {
		InvalidLuceneQueryException ilqe = new InvalidLuceneQueryException("Bad Lucene Query");
		assertNotNull(ilqe);
		assertEquals("Bad Lucene Query", ilqe.getMessage());
	}

}
