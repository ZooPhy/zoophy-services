package edu.asu.zoophy.pipeline;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Responsible for running ZooPhy jobs
 * @author devdemetri
 */
public class ZooPhyRunner {
	
	private ZooPhyJob job;
	private ZooPhyMailer mailer;
	private Logger log;
	
	/**
	 * Map for tracking running jobs
	 * Key - generated JobID
	 * Value - server PID
	 */
	private static Map<String, Integer> ids = new ConcurrentHashMap<String, Integer>();

	public ZooPhyRunner(String replyEmail, String jobName) throws PipelineException {
		log = Logger.getLogger("ZooPhyRunner");
		log.info("Initializing ZooPhy Job");
		job = new ZooPhyJob(generateID(),jobName,replyEmail);
	}
	
	/**
	 * Runs the ZooPhy pipeline on the given Accessions
	 * @param accessions
	 * @throws PipelineException
	 */
	public void runZooPhy(List<String> accessions) throws PipelineException {
		try {
			log.info("Initializing ZooPhyMailer... : "+job.getID());
			mailer = new ZooPhyMailer(job);
			log.info("Sending Start Email... : "+job.getID());
			mailer.sendStartEmail();
			log.info("Initializing Sequence Aligner... : "+job.getID());
			SequenceAligner aligner = new SequenceAligner(job);
			log.info("Running Sequence Aligner... : "+job.getID());
			aligner.align(accessions);
			log.info("Initializing Beast Runner... : "+job.getID());
			BeastRunner beast = new BeastRunner(job, mailer);
			log.info("Starting Beast Runner... : "+job.getID());
			beast.run();
			log.info("Sending Results Email... : "+job.getID());
			mailer.sendSuccessEmail();
			log.info("ZooPhy Job Complete: "+job.getID());
		}
		catch (PipelineException pe) {
			log.log(Level.SEVERE, "PipelineException for job: "+job.getID()+" : "+pe.getMessage());
			log.info("Sending Failure Email... : "+job.getID());
			mailer.sendFailureEmail(pe.getUserMessage());
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Unhandled Exception for job: "+job.getID()+" : "+e.getMessage());
			log.info("Sending Failure Email... : "+job.getID());
			mailer.sendFailureEmail("Internal Server Error");
		}
	}
	
	/**
	 * Update the pid for a ZooPhyJob
	 * @param jobID
	 * @param pid
	 */
	protected static void setPID(String jobID, int pid) {
		ids.put(jobID, pid);
	}
	
	/**
	 * Generates a UUID to be used as a jobID
	 * @return Unused UUID
	 * @throws PipelineException 
	 */
	private String generateID() throws PipelineException {
		try {
			log.info("Generating UID...");
			String id  = java.util.UUID.randomUUID().toString();
			log.info("Trying ID: "+id);
			while (ids.keySet().contains(id)) {
				id  = java.util.UUID.randomUUID().toString();
			}
			log.info("Assigned ID: "+id);
			ids.put(id, 0);
			return id;
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Error generating job ID: "+e.getMessage());
			throw new PipelineException("Error generating job ID: "+e.getMessage(), "Failed to start ZooPhy Job!");
		}
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
