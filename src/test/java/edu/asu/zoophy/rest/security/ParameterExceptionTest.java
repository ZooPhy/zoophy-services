package edu.asu.zoophy.rest.security;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ParameterExceptionTest {

	@Test
	public void test() {
		ParameterException pe = new ParameterException("Accession: ABC12345");
		assertNotNull(pe);
		assertEquals("Bad Parameter: Accession: ABC12345", pe.getMessage());
	}

}
