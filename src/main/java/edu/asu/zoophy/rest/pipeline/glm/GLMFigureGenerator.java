package edu.asu.zoophy.rest.pipeline.glm;

import java.io.File;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import edu.asu.zoophy.rest.pipeline.PipelineException;
import edu.asu.zoophy.rest.pipeline.PropertyProvider;
import edu.asu.zoophy.rest.pipeline.ZooPhyJob;

/**
 * Uses R scripts created by Dan to generate figures for GLM results
 * @author devdemetri
 */
public class GLMFigureGenerator {

	private final ZooPhyJob job;
	private final String JOB_LOG_DIR;
	private final String BEAST_SCRIPTS_DIR;
	private final String JOB_WORK_DIR;
	private final String DAN_R_SCRIPT;
	private final String DAN_BASH_SCRIPT;
	private final Logger log;
	private final File logFile;
	private final String baseName;
	private Set<String> filesToCleanup = null;
	
	public GLMFigureGenerator(final ZooPhyJob job) throws PipelineException {
		PropertyProvider provider = PropertyProvider.getInstance();
		JOB_LOG_DIR = provider.getProperty("job.logs.dir");
		BEAST_SCRIPTS_DIR = provider.getProperty("beast.scripts.dir");
		DAN_R_SCRIPT = System.getProperty("user.dir")+"/make_predictor_fig.R";
		DAN_BASH_SCRIPT = System.getProperty("user.dir")+"/make_table.sh";
		JOB_WORK_DIR = System.getProperty("user.dir")+"/ZooPhyJobs/"+job.getID()+"/";;
		filesToCleanup = new LinkedHashSet<String>();
		log = Logger.getLogger("GLMFigureGenerator"+job.getID());
		this.job = job;
		baseName = JOB_WORK_DIR+job.getID()+"_GLMedits_states";
		logFile = new File(JOB_LOG_DIR+job.getID()+".log");
	}
	
	/**
	 * Generates PDF Figure from GLM results
	 * @return GLM Figure File
	 * @throws GLMException
	 */
	public File generateFigure() throws GLMException {
		File figure = null;
		FileHandler fileHandler = null;
		try {
			fileHandler = new FileHandler(JOB_LOG_DIR+job.getID()+".log", true);
			SimpleFormatter formatter = new SimpleFormatter();
	        fileHandler.setFormatter(formatter);
	        log.addHandler(fileHandler);
	        log.setUseParentHandlers(false);
			log.info("Starting the GLM Figure Generator process...");
			String analyserOutput = runLogAnalyser();
			figure = runDanScripts(analyserOutput);
			if (figure.exists()) {
				return figure;
			}
			else {
				log.log(Level.SEVERE, "GLM Figure Generator did not generate a PDF at "+figure.getAbsolutePath());
				throw new GLMException("GLM Figure Generator did not generate a PDF at "+figure.getAbsolutePath(), null);
			}
		}
		catch (GLMException glme) {
			throw glme;
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "ERROR running GLM Figure Generator: "+e.getMessage());
			throw new GLMException("ERROR running GLM Figure Generator: "+e.getMessage(), null);
		}
		finally {
			cleanupGLM();
			if (fileHandler != null) {
				fileHandler.close();
			}
		}
	}

	/**
	 * Runs the BEAST Log Analyser script
	 * @return file path to Log Analyser output file
	 * @throws GLMException
	 */
	private String runLogAnalyser() throws GLMException {
		String glmLog = baseName+".model.log";
		String analyserOutput = baseName+"_logAnalyser_output.txt";
		filesToCleanup.add(glmLog);
		try {
			String logAnalyser = BEAST_SCRIPTS_DIR+"loganalyser";
			log.info("Running Log Analyser...");
			ProcessBuilder builder = new ProcessBuilder(logAnalyser, glmLog, analyserOutput);
			builder.redirectOutput(Redirect.appendTo(logFile));
			builder.redirectError(Redirect.appendTo(logFile));
			log.info("Starting Process: "+builder.command().toString());
			Process logAnalyserProcess = builder.start();
			logAnalyserProcess.waitFor();
			if (logAnalyserProcess.exitValue() != 0) {
				log.log(Level.SEVERE, "Log Analyser failed! with code: "+logAnalyserProcess.exitValue());
				throw new GLMException("Log Analyser failed! with code: "+logAnalyserProcess.exitValue(), null);
			}
			log.info("Log Analyser finished.");
			filesToCleanup.add(analyserOutput);
			return analyserOutput;
		}
		catch (GLMException glme) {
			throw glme;
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Error running Log Analyser: "+e.getMessage());
			throw new GLMException("Error running Log Analyser: "+e.getMessage(), null);
		}
	}
	
	/**
	 * Runs Dan's bash and R scripts to create the GLM Figure
	 * @param analyserOutput
	 * @return Resulting PDF file
	 * @throws GLMException
	 */
	private File runDanScripts(String analyserOutput) throws GLMException {
		String cleanedData = baseName+"_summary_clean.txt";
		String predictorData = JOB_WORK_DIR+job.getID()+"_predictorNames.txt";
		String figurePath = baseName+"_figure.pdf";
		try {
			ProcessBuilder bashBuilder = new ProcessBuilder(DAN_BASH_SCRIPT, analyserOutput, cleanedData);
			bashBuilder.redirectOutput(Redirect.appendTo(logFile));
			bashBuilder.redirectError(Redirect.appendTo(logFile));
			log.info("Starting Process: "+bashBuilder.command().toString());
			Process bashProcess = bashBuilder.start();
			bashProcess.waitFor();
			if (bashProcess.exitValue() != 0) {
				log.log(Level.SEVERE, "make_table.sh failed! with code: "+bashProcess.exitValue());
				throw new GLMException("make_table.sh failed! with code: "+bashProcess.exitValue(), null);
			}
			log.info("make_table.sh finished.");
			filesToCleanup.add(cleanedData);
			ProcessBuilder rBuilder = new ProcessBuilder("Rscript", "--vanilla", DAN_R_SCRIPT, cleanedData, predictorData, figurePath);
			rBuilder.redirectOutput(Redirect.appendTo(logFile));
			rBuilder.redirectError(Redirect.appendTo(logFile));
			log.info("Starting Process: "+rBuilder.command().toString());
			Process rProcess = rBuilder.start();
			rProcess.waitFor();
			if (rProcess.exitValue() != 0) {
				log.log(Level.SEVERE, "R script failed! with code: "+rProcess.exitValue());
				throw new GLMException("R script failed! with code: "+rProcess.exitValue(), null);
			}
			log.info("R script finished.");
			filesToCleanup.add(predictorData);
			File figureFile = new File(figurePath);
			return figureFile;
		}
		catch (GLMException glme) {
			throw glme;
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Error running Dan's GLM figure scripts: "+e.getMessage());
			throw new GLMException("Error running Dan's GLM figure scripts: "+e.getMessage(), null);
		}
	}
	
	/**
	 * Cleans up GLM files
	 */
	private void cleanupGLM() {
		log.info("Cleaning up GLM files...");
		try {
			Path fileToDelete;
			for (String filePath : filesToCleanup) {
				try {
					log.warning("Deleting: "+filePath);
					fileToDelete = Paths.get(filePath);
					Files.delete(fileToDelete);
				}
				catch (Exception e) {
					log.warning("Could not delete: "+filePath+" : "+e.getMessage());
				}
			}
			filesToCleanup.clear();
			log.info("Cleanup complete.");
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "GLM cleanup failed: "+e.getMessage());
		}
	}
	
}
