package edu.asu.zoophy.rest.pipeline;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.asu.zoophy.rest.custom.FastaRecord;
import edu.asu.zoophy.rest.database.ZooPhyDAO;
import edu.asu.zoophy.rest.genbank.JobAccessions;
import edu.asu.zoophy.rest.index.LuceneHierarchySearcher;
import edu.asu.zoophy.rest.pipeline.glm.GLMFigureGenerator;
import edu.asu.zoophy.rest.pipeline.glm.Predictor;

/**
 * Responsible for running ZooPhy jobs
 * @author devdemetri, kbhangal, amagge
 */
public class ZooPhyRunner {

	private final ZooPhyJob job;
	private final ZooPhyMailer mailer;
	private final Logger log;

	public ZooPhyRunner(String replyEmail, String jobName, boolean useGLM, Map<String, List<Predictor>> predictors,
						String disjoinerLevel, boolean useGeoUncertainties,
						XMLParameters xmlOptions) throws PipelineException {
		char rand_char = (char) (Math.random()*26 + 'a');				
		final String id  = rand_char + UUID.randomUUID().toString();	//Used as property by SpreaD3, hence start with char
		log = Logger.getLogger("ZooPhyRunner"+id);
		log.info("Initializing ZooPhy Job");
		job = new ZooPhyJob(id, jobName, replyEmail, useGLM, predictors, disjoinerLevel, useGeoUncertainties, xmlOptions);
		log.info("Initializing ZooPhyMailer... : "+job.getID());
		mailer = new ZooPhyMailer(job);
	}

	/**
	 * Runs the ZooPhy pipeline on the given Accessions
	 * @param accessions
	 * @param dao 
	 * @param hierarchyIndexSearcher 
	 * @throws PipelineException
	 */
	public void runZooPhy(List<String> accessions, List<FastaRecord> fastaRecords, ZooPhyDAO dao, LuceneHierarchySearcher hierarchyIndexSearcher) throws PipelineException {
		try {
			JobAccessions jobAccessions;
			log.info("Sending Start Email... : "+job.getID());
			List<File> fileList = null;
			mailer.sendStartEmail(fileList);
			log.info("Initializing Sequence Aligner... : "+job.getID());
			SequenceAligner aligner = new SequenceAligner(job, dao, hierarchyIndexSearcher);
			log.info("Running Sequence Aligner... : "+job.getID());
			jobAccessions = aligner.align(accessions, fastaRecords, false);
			log.info("Initializing Beast Runner... : "+job.getID());
			BeastRunner beast = new BeastRunner(job, mailer, jobAccessions.getDistinctLocations());
			log.info("Starting Beast Runner... : "+job.getID());
			// Run BEAST and get list of output files
			List<File> resultsList = beast.run();
			if (job.isUsingGLM()) {
				log.info("Running GLM Figure Generator... : "+job.getID());
				GLMFigureGenerator figureGenerator = new GLMFigureGenerator(job);
				File glmFile = figureGenerator.generateFigure();
				resultsList.add(glmFile);
			}
			log.info("Sending Results Email... : "+job.getID());
			mailer.sendSuccessEmail(resultsList); 
			PipelineManager.removeProcess(job.getID());
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
	 * @return generated ID for the ZooPhy job being run
	 */
	public String getJobID() {
		return job.getID();
	}
	
	/**
	 * Runs early stages of the pipeline to test ZooPhy job viability
	 * @param accessions
	 * @param dao
	 * @param hierarchyIndexSearcher
	 * @throws PipelineException
	 */
	public JobAccessions testZooPhy(List<String> accessions, List<FastaRecord> fastaRecords, ZooPhyDAO dao, LuceneHierarchySearcher hierarchyIndexSearcher) throws PipelineException {
		try {
			JobAccessions jobAccessions;
			log.info("Initializing test Sequence Aligner... : "+job.getID());
			SequenceAligner aligner = new SequenceAligner(job, dao, hierarchyIndexSearcher);
			log.info("Running test Sequence Aligner... : "+job.getID());
			jobAccessions = aligner.align(accessions, fastaRecords, true);
			log.info("Initializing test Beast Runner... : "+job.getID());
			BeastRunner beast = new BeastRunner(job, null, jobAccessions.getDistinctLocations());
			log.info("Starting test Beast Runner... : "+job.getID());
			beast.test();
			log.info("ZooPhy Job Test completed successfully: "+job.getID());
			
			return jobAccessions;
		}
		catch (PipelineException pe) {
			log.log(Level.SEVERE, "PipelineException for test job: "+job.getID()+" : "+pe.getMessage());
			throw pe;
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Unhandled Exception for test job: "+job.getID()+" : "+e.getMessage());
			throw new PipelineException("Unhandled Exception: "+e.getMessage(), null);
		}	
	}
}
