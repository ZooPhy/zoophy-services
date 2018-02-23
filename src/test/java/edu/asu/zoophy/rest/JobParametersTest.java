package edu.asu.zoophy.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import edu.asu.zoophy.rest.custom.JobRecord;
import edu.asu.zoophy.rest.pipeline.XMLParameters;
import edu.asu.zoophy.rest.pipeline.glm.Predictor;

@RunWith(SpringRunner.class)
@SpringBootTest
public class JobParametersTest {

	@Test
	public void testParameters() {
		final List<JobRecord> mockRecords = new LinkedList<JobRecord>();
		final Map<String, List<Predictor>> mockPredictors = new LinkedHashMap<String, List<Predictor>>();
		Predictor mockPredictor = new Predictor();
		mockPredictor.setName("temp");
		mockPredictor.setState("az");
		mockPredictor.setValue(121.7);
		mockPredictor.setYear(2010);
		List<Predictor> tempList = new LinkedList<Predictor>();
		tempList.add(mockPredictor);
		mockPredictors.put("AZ", tempList);
		final XMLParameters mockXMLParams = XMLParameters.getDefault();
		JobParameters paramters = new JobParameters();
		
		//Fasta
		JobRecord jobRecord = new JobRecord();
		jobRecord.setId("ABC");
		jobRecord.setCollectionDate("25-Mar-2006");
		jobRecord.setGeonameID("5308655");
		jobRecord.setRawSequence("ATGGAGAAAATAGTGCTTCTTTTTGCAATAGTCAGTCTTGTAAAAGTGATCAGATTTGCAT");
		jobRecord.setResourceSource("2");
		mockRecords.add(jobRecord);
		
		//GenBank
		jobRecord = new JobRecord();
		jobRecord.setId("CY214007");
		jobRecord.setResourceSource("1");
		mockRecords.add(jobRecord);
		
		paramters.setRecords(mockRecords);
		paramters.setJobName("Mock Job");
		paramters.setPredictors(mockPredictors);
		paramters.setReplyEmail("zoophy@asu.edu");
		paramters.setUseGLM(true);
		paramters.setXmlOptions(mockXMLParams);
		assertThat(paramters).isNotNull();
		assertThat(paramters.getRecords().get(0).getId()).isNotNull();
		assertThat(paramters.getRecords().get(0).getRawSequence()).isNotNull();
		assertThat(paramters.getRecords().get(1).getRawSequence()).isNull();
		assertThat(paramters.getRecords()).isEqualTo(mockRecords);
		assertThat(paramters.getJobName()).isNotNull();
		assertThat(paramters.getJobName()).isEqualTo("Mock Job");
		assertThat(paramters.getPredictors()).isNotNull();
		assertThat(paramters.getPredictors()).isEqualTo(mockPredictors);
		assertThat(paramters.getReplyEmail()).isNotNull();
		assertThat(paramters.getReplyEmail()).isEqualTo("zoophy@asu.edu");
		assertThat(paramters.isUsingGLM()).isNotNull();
		assertThat(paramters.isUsingGLM()).isEqualTo(true);
		assertThat(paramters.getXmlOptions()).isNotNull();
		assertThat(paramters.getXmlOptions()).isEqualTo(mockXMLParams);
	}

}
