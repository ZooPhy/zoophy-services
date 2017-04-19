package edu.asu.zoophy.rest.index;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DocumentMapperTest {
	
	@Test
	public void test() {
		DocumentMapper mapper = new DocumentMapper();
		assertNotNull(mapper);
		// the main point of this class is the static method, which gets tested primarily in Lucene Searcher. Will revisit this later.
	}

}
