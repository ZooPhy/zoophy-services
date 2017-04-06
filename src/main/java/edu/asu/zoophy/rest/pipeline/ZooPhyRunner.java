package edu.asu.zoophy.rest.pipeline;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.asu.zoophy.rest.database.ZooPhyDAO;
import edu.asu.zoophy.rest.genbank.GenBankRecord;
import edu.asu.zoophy.rest.index.LuceneSearcher;
import edu.asu.zoophy.rest.pipeline.glm.GLMFigureGenerator;
import edu.asu.zoophy.rest.pipeline.glm.Predictor;

/**
 * Responsible for running ZooPhy jobs
 * @author devdemetri
 */
public class ZooPhyRunner {

	private final ZooPhyJob job;
	private final ZooPhyMailer mailer;
	private final Logger log;

	public ZooPhyRunner(String replyEmail, String jobName, boolean useGLM, Map<String, List<Predictor>> predictors, XMLParameters xmlOptions) throws PipelineException {
		log = Logger.getLogger("ZooPhyRunner");
		log.info("Initializing ZooPhy Job");
		job = new ZooPhyJob(generateID(),jobName,replyEmail, useGLM, predictors, xmlOptions);
		log.info("Initializing ZooPhyMailer... : "+job.getID());
		mailer = new ZooPhyMailer(job);
	}
	
	/**
	 * Runs the ZooPhy pipeline on the given Accessions
	 * @param accessions
	 * @param dao 
	 * @param indexSearcher 
	 * @throws PipelineException
	 */
	public void runZooPhy(List<String> accessions, ZooPhyDAO dao, LuceneSearcher indexSearcher) throws PipelineException {
		try {
			log.info("Sending Start Email... : "+job.getID());
			mailer.sendStartEmail();
			log.info("Initializing Sequence Aligner... : "+job.getID());
			SequenceAligner aligner = new SequenceAligner(job, dao, indexSearcher);
			log.info("Running Sequence Aligner... : "+job.getID());
			aligner.align(accessions, false);
			log.info("Initializing Beast Runner... : "+job.getID());
			BeastRunner beast = new BeastRunner(job, mailer);
			log.info("Starting Beast Runner... : "+job.getID());
			File treeFile = beast.run();
			File[] results = new File[2];
			results[0] = treeFile;
			if (job.isUsingGLM()) {
				log.info("Running GLM Figure Generator... : "+job.getID());
				GLMFigureGenerator figureGenerator = new GLMFigureGenerator(job);
				File glmFile = figureGenerator.generateFigure();
				results[1] = glmFile;
			}
			else {
				results[1] = null;
			}
			log.info("Sending Results Email... : "+job.getID());
			mailer.sendSuccessEmail(results); 
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
	 * Generates a UUID to be used as a jobID
	 * @return randomly generated UUID
	 * @throws PipelineException 
	 */
	private String generateID() throws PipelineException {
		try {
			log.info("Generating UID...");
			String id  = java.util.UUID.randomUUID().toString();
			log.info("Assigned ID: "+id);
			return id;
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Error generating job ID: "+e.getMessage());
			throw new PipelineException("Error generating job ID: "+e.getMessage(), "Failed to start ZooPhy Job!");
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
	 * @param indexSearcher
	 * @throws PipelineException
	 */
	public Set<String> testZooPhy(List<String> accessions, ZooPhyDAO dao, LuceneSearcher indexSearcher) throws PipelineException {
		try {
			log.info("Initializing test Sequence Aligner... : "+job.getID());
			SequenceAligner aligner = new SequenceAligner(job, dao, indexSearcher);
			log.info("Running test Sequence Aligner... : "+job.getID());
			final List<GenBankRecord> finalRecs = aligner.align(accessions, true);
			log.info("Initializing test Beast Runner... : "+job.getID());
			BeastRunner beast = new BeastRunner(job, null);
			log.info("Starting test Beast Runner... : "+job.getID());
			beast.test();
			log.info("ZooPhy Job Test completed successfully: "+job.getID());
			Set<String> usedAccessions = new HashSet<String>((int)(finalRecs.size()*1.1), 1.0f);
			for (GenBankRecord rec : finalRecs) {
				usedAccessions.add(rec.getAccession());
			}
			return usedAccessions;
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
