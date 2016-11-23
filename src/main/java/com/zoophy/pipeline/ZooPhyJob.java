package com.zoophy.pipeline;

/**
 * Encapsulates related info for a ZooPhy Job
 * @author devdemetri
 */
public final class ZooPhyJob {
	
	private String id;
	private String jobName;
	private String replyEmail;
	
	public ZooPhyJob(String id, String name, String email) {
		this.id = id;
		this.jobName = name;
		this.replyEmail = email;
	}
	
	public String getID() {
		return id;
	}
	
	public String getJobName() {
		return jobName;
	}
	
	public String getReplyEmail() {
		return replyEmail;
	}
	
}
