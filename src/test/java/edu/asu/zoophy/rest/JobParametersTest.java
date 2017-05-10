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

import edu.asu.zoophy.rest.pipeline.XMLParameters;
import edu.asu.zoophy.rest.pipeline.glm.Predictor;

@RunWith(SpringRunner.class)
@SpringBootTest
public class JobParametersTest {

	@Test
	public void testParameters() {
		final List<String> mockAccessions = new LinkedList<String>();
		mockAccessions.add("ABC");
		mockAccessions.add("DEF");
		mockAccessions.add("GHI");
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
		paramters.setAccessions(mockAccessions);
		paramters.setJobName("Mock Job");
		paramters.setPredictors(mockPredictors);
		paramters.setReplyEmail("zoophy@asu.edu");
		paramters.setUseGLM(true);
		paramters.setXmlOptions(mockXMLParams);
		assertThat(paramters).isNotNull();
		assertThat(paramters.getAccessions()).isNotNull();
		assertThat(paramters.getAccessions()).isEqualTo(mockAccessions);
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
