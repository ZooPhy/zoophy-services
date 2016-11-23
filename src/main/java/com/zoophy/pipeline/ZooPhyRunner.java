package com.zoophy.pipeline;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Responsible for running ZooPhy jobs
 * @author devdemetri
 */
public class ZooPhyRunner {
	
	private String replyEmail;
	private String jobID;
	private String jobName;
	
	private static Set<String> ids = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
	
	public ZooPhyRunner(String replyEmail) {
		this.replyEmail = replyEmail;
		this.jobName = null;
		this.jobID = generateID();
	}

	public ZooPhyRunner(String replyEmail, String jobName) {
		this.replyEmail = replyEmail;
		this.jobName = jobName;
		this.jobID = generateID();
	}
	
	public void runZooPhy(List<String> accessions) {
		//TODO: run the pipeline//
	}
	
	private static String generateID() {
		String id  = java.util.UUID.randomUUID().toString();
		while (ids.contains(id)) {
			id  = java.util.UUID.randomUUID().toString();
		}
		ids.add(id);
		return id;
	}

}
