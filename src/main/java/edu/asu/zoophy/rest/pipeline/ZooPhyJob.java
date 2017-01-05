package edu.asu.zoophy.rest.pipeline;

/**
 * Encapsulates related info for a ZooPhy Job
 * @author devdemetri
 */
public final class ZooPhyJob {
	
	private final String ID;
	private final String JOB_NAME;
	private final String REPLY_EMAIL;
	
	public ZooPhyJob(String id, String name, String email) {
		ID = id;
		JOB_NAME = name;
		REPLY_EMAIL = email;
	}
	
	public String getID() {
		return ID;
	}
	
	public String getJobName() {
		return JOB_NAME;
	}
	
	public String getReplyEmail() {
		return REPLY_EMAIL;
	}
	
}
