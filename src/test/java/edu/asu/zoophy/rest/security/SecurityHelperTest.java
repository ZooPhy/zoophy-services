package edu.asu.zoophy.rest.security;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import edu.asu.zoophy.rest.pipeline.BeastSubstitutionModel;
import edu.asu.zoophy.rest.pipeline.XMLParameters;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SecurityHelperTest {
	
	@Autowired
	SecurityHelper helper;
	
	@Test
	public void testParamters() {
		assertNotNull(helper);
		assertFalse(helper.checkParameter("", Parameter.ACCESSION));
		assertFalse(helper.checkParameter(null, Parameter.ACCESSION));
		assertFalse(helper.checkParameter("ABC-12345", Parameter.ACCESSION));
		assertTrue(helper.checkParameter("ABC12345", Parameter.ACCESSION));
		assertFalse(helper.checkParameter("spammy@spam", Parameter.EMAIL));
		assertTrue(helper.checkParameter("sparky@asu.edu", Parameter.EMAIL));
		assertFalse(helper.checkParameter("1234567890", Parameter.JOB_ID));
		assertTrue(helper.checkParameter("a12345678-1234-1234-1234-123456789abc", Parameter.JOB_ID));
		assertFalse(helper.checkParameter("T Virus <Outbreak/> 2018", Parameter.JOB_NAME));
		assertTrue(helper.checkParameter("T Virus Outbreak 2018", Parameter.JOB_NAME));
		assertFalse(helper.checkParameter("Accession:--DROP TABLES-- AND TaxonID:9606", Parameter.LUCENE_QUERY));
		assertTrue(helper.checkParameter("Accession:ABC12345 AND TaxonID:9606", Parameter.LUCENE_QUERY));
	}
	
	@Test
	public void testNullXMLOptions() {
		assertNotNull(helper);
		try {
			helper.verifyXMLOptions(null);
		}
		catch (Exception e) {
			assertEquals(ParameterException.class, e.getClass());
			assertEquals("Bad Parameter: Missing XML Parameters!", e.getMessage());
		}
		XMLParameters xmlParams = new XMLParameters();
		assertNotNull(xmlParams);
		assertFalse(xmlParams.isDefault());
		try {
			helper.verifyXMLOptions(xmlParams);
		}
		catch (Exception e) {
			assertEquals(ParameterException.class, e.getClass());
			assertEquals("Bad Parameter: Missing XML Chain Length!", e.getMessage());
		}
		xmlParams.setChainLength(50);
		try {
			helper.verifyXMLOptions(xmlParams);
		}
		catch (Exception e) {
			assertEquals(ParameterException.class, e.getClass());
			assertEquals("Bad Parameter: Invalid XML Chain Length!", e.getMessage());
		}
		xmlParams.setChainLength(800000000);
		try {
			helper.verifyXMLOptions(xmlParams);
		}
		catch (Exception e) {
			assertEquals(ParameterException.class, e.getClass());
			assertEquals("Bad Parameter: Invalid XML Chain Length!", e.getMessage());
		}
		xmlParams.setChainLength(20000000);
		try {
			helper.verifyXMLOptions(xmlParams);
		}
		catch (Exception e) {
			assertEquals(ParameterException.class, e.getClass());
			assertEquals("Bad Parameter: Missing XML Sub Sample Rate!", e.getMessage());
		}
		xmlParams.setSubSampleRate(2000000);
		try {
			helper.verifyXMLOptions(xmlParams);
		}
		catch (Exception e) {
			assertEquals(ParameterException.class, e.getClass());
			assertEquals("Bad Parameter: Invalid XML Sub Sample Rate!", e.getMessage());
		}
		xmlParams.setSubSampleRate(20);
		try {
			helper.verifyXMLOptions(xmlParams);
		}
		catch (Exception e) {
			assertEquals(ParameterException.class, e.getClass());
			assertEquals("Bad Parameter: Invalid XML Sub Sample Rate!", e.getMessage());
		}
		xmlParams.setSubSampleRate(20000);
		try {
			helper.verifyXMLOptions(xmlParams);
		}
		catch (Exception e) {
			assertEquals(ParameterException.class, e.getClass());
			assertEquals("Bad Parameter: Missing XML Substitution Model!", e.getMessage());
		}
		xmlParams.setSubstitutionModel(BeastSubstitutionModel.HKY);
		try {
			helper.verifyXMLOptions(xmlParams);
		}
		catch (Exception e) {
			fail("Should have passed XML verification");
		}
		xmlParams = XMLParameters.getDefault();
		try {
			helper.verifyXMLOptions(xmlParams);
		}
		catch (Exception e) {
			fail("Should have passed XML verification");
		}
	}

}
