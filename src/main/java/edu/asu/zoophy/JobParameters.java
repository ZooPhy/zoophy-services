package edu.asu.zoophy;

import java.util.List;

/**
 * Object to encapsulate parameters for ZooPhy jobs
 * @author devdemetri
 */
public class JobParameters {
	
	private String replyEmail;
	private String jobName;
	private List<String> accessions;
	
	public JobParameters() {
		
	}

	public String getReplyEmail() {
		return replyEmail;
	}

	public void setReplyEmail(String replyEmail) {
		this.replyEmail = replyEmail;
	}

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public List<String> getAccessions() {
		return accessions;
	}

	public void setAccessions(List<String> accessions) {
		this.accessions = accessions;
	}
	
}
