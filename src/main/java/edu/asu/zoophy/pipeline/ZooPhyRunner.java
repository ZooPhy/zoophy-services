package edu.asu.zoophy.pipeline;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Responsible for running ZooPhy jobs
 * @author devdemetri
 */
public class ZooPhyRunner {
	
	private ZooPhyJob job;
	private ZooPhyMailer mailer;
	
	/**
	 * Map for tracking running jobs
	 * Key - generated JobID
	 * Value - server PID
	 */
	private static Map<String, Integer> ids = new ConcurrentHashMap<String, Integer>();
	
	public ZooPhyRunner(String replyEmail) {
		job = new ZooPhyJob(generateID(),null,replyEmail);
	}

	public ZooPhyRunner(String replyEmail, String jobName) {
		job = new ZooPhyJob(generateID(),jobName,replyEmail);
	}
	
	/**
	 * Runs the ZooPhy pipeline on the given Accessions
	 * @param accessions
	 * @throws PipelineException
	 */
	public void runZooPhy(List<String> accessions) throws PipelineException {
		try {
			mailer = new ZooPhyMailer(job);
			mailer.sendStartEmail();
			SequenceAligner aligner = new SequenceAligner(job);
			aligner.align(accessions);
			BeastRunner beast = new BeastRunner(job, mailer);
			beast.run();
			mailer.sendSuccessEmail();
		}
		catch (PipelineException pe) {
			mailer.sendFailureEmail(pe.getUserMessage());
		}
		catch (Exception e) {
			mailer.sendFailureEmail("Internal Server Error");
		}
	}
	
	/**
	 * Update the pid for a ZooPhyJob
	 * @param jobID
	 * @param pid
	 */
	protected static void setPID(String jobID, Integer pid) {
		ids.put(jobID, pid);
	}
	
	/**
	 * Generates a UUID to be used as a jobID
	 * @return Unused UUID
	 */
	private static String generateID() {
		String id  = java.util.UUID.randomUUID().toString();
		while (ids.keySet().contains(id)) {
			id  = java.util.UUID.randomUUID().toString();
		}
		ids.put(id, null);
		return id;
	}
	
	/**
	 * Kills the given ZooPhy Job. NOTE: Currently only works on Unix based systems, NOT Windows.
	 * @param jobID - ID of ZooPhy job to kill
	 * @throws PipelineException if the job does not exist
	 */
	public static void killJob(String jobID) throws PipelineException {
		try {
			Integer pid = ids.get(jobID);
			if (pid == null || pid < 100) {
				throw new PipelineException("ERROR! Tried to kill non-existent job: "+jobID, "Job Does Not Exist!");
			}
			ProcessBuilder builder = new ProcessBuilder("kill", "-9", pid.toString());
			Process killProcess = builder.start();
			killProcess.waitFor();
			if (killProcess.exitValue() != 0) {
				throw new PipelineException("ERROR! Could not kill job: "+jobID+" with code: "+killProcess.exitValue(), "Could Not Kill Job!");
			}
		}
		catch (Exception e) {
			throw new PipelineException("ERROR! Could not kill job: "+jobID+" : "+e.getMessage(), "Could Not Kill Job!");
		}
	}

}
