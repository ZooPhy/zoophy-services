package edu.asu.zoophy.rest.pipeline;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import edu.asu.zoophy.rest.database.ZoophyDAO;
import edu.asu.zoophy.rest.index.LuceneSearcher;

/**
 * Manages ZooPhy Pipeline jobs
 * @author devdemetri
 */
@EnableAsync
@Component("PipelineManager")
public class PipelineManager {
	
	@Autowired
	private ZoophyDAO dao;
	
	@Autowired
	private LuceneSearcher indexSearcher;
	
	private static Logger log = Logger.getLogger("PipelineManager");
	
	/**
	 * Map for tracking running jobs
	 * Key - generated JobID
	 * Value - server Process
	 */
	private static Map<String, Process> processes = new ConcurrentHashMap<String, Process>();
	
	 /**
     * Asynchronously a the ZooPhy job
     * @param runner - ZoophyRunner containing the job details
     * @param accessions - list of accessions for the job
     * @throws PipelineException
     */
    @Async
    public void startZooPhyPipeline(ZooPhyRunner runner, List<String> accessions) throws PipelineException {
    	log.info("Starting ZooPhy Job: "+runner.getJobID());
    	runner.runZooPhy(accessions, dao, indexSearcher);
    }
	
	/**
	 * Update the Process for a ZooPhyJob
	 * @param jobID
	 * @param jobProcess
	 */
	protected static void setProcess(String jobID, Process jobProcess) {
		processes.put(jobID, jobProcess);
	}
	
	/**
	 * Remove the Process for a finished or stopped ZooPhyJob
	 * @param jobID
	 * @return True if the Process existed, False otherwise
	 */
	protected static boolean removeProcess(String jobID) {
		return (processes.remove(jobID) != null);
	}
	
	/**
	 * Check if the Job is still running
	 * @param jobID
	 * @return True if it has not been stopped/finished, False otherwise
	 */
	protected static boolean checkProcess(String jobID) {
		return (processes.get(jobID) != null);
	}
	
	/**
	 * Kills the given ZooPhy Job. NOTE: Currently only works on Unix based systems, NOT Windows.
	 * @param jobID - ID of ZooPhy job to kill
	 * @throws PipelineException if the job does not exist
	 */
	 public void killJob(String jobID) throws PipelineException {
		try {
			Process jobProcess = processes.remove(jobID);
			if (jobProcess != null) {
				log.info("Killing job: "+jobID);
				jobProcess.destroy();
			}
			else {
				log.warning("Attempted to kill non-existent job: "+jobID);
				throw new PipelineException("ERROR! Tried to kill non-existent job: "+jobID, "Job Does Not Exist!");
			}
		}
		catch (Exception e) {
			log.warning("Could not kill job: "+jobID+" : "+e.getMessage());
			throw new PipelineException("ERROR! Could not kill job: "+jobID+" : "+e.getMessage(), "Could Not Kill Job!");
		}
	}
	
}
