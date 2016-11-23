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
	
	private ZooPhyJob job;
	private ZooPhyMailer mailer;
	
	private static Set<String> ids = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
	
	public ZooPhyRunner(String replyEmail) {
		job = new ZooPhyJob(generateID(),null,replyEmail);
	}

	public ZooPhyRunner(String replyEmail, String jobName) {
		job = new ZooPhyJob(generateID(),jobName,replyEmail);
	}
	
	public void runZooPhy(List<String> accessions) throws PipelineException {
		try {
			mailer = new ZooPhyMailer(job);
			mailer.sendStartEmail();
			//TODO: add rest of pipeline
			// mailer.sendSuccessEmail();
		}
		catch (PipelineException pe) {
			mailer.sendFailureEmail(pe.getUserMessage());
		}
		catch (Exception e) {
			mailer.sendFailureEmail("Server Error");
		}
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
