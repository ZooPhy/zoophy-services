package edu.asu.zoophy.pipeline;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import edu.asu.zoophy.database.ZoophyDAO;
import edu.asu.zoophy.index.LuceneSearcher;

/**
 * Manages ZooPhy Pipeline jobs
 * @author devdemetri
 */
@Repository("PipelineManager")
public class PipelineManager {
	
	@Autowired
	private ZoophyDAO dao;
	
	@Autowired
	private LuceneSearcher indexSearcher;
	
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
	 * Remove the Process for a finished ZooPhyJob
	 * @param jobID
	 */
	protected static void removeProcess(String jobID) {
		processes.remove(jobID);
	}
	
	/**
	 * Kills the given ZooPhy Job. NOTE: Currently only works on Unix based systems, NOT Windows.
	 * @param jobID - ID of ZooPhy job to kill
	 * @throws PipelineException if the job does not exist
	 */
	public static void killJob(String jobID) throws PipelineException {
		try {
			Process jobProcess = processes.get(jobID);
			if (jobProcess == null || !jobProcess.isAlive()) {
				throw new PipelineException("ERROR! Tried to kill non-existent job: "+jobID, "Job Does Not Exist!");
			}
			jobProcess.destroy();
		}
		catch (Exception e) {
			throw new PipelineException("ERROR! Could not kill job: "+jobID+" : "+e.getMessage(), "Could Not Kill Job!");
		}
	}
	
}