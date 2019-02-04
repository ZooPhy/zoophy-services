package edu.asu.zoophy.rest.pipeline;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;

import edu.asu.zoophy.rest.pipeline.glm.GLMException;

/**
 * Responsible for running BEAST processes
 * @author devdemetri, amagge
 */
public class BeastRunner {
	
	private final String JOB_LOG_DIR;
	private final String BEAST_SCRIPTS_DIR;
	private final String SPREAD3;
	private final String WORLD_GEOJSON;
	private final String ZOOPHY_VIZ;
	private final String FIGTREE_TEMPLATE;
	private final String GLM_SCRIPT;
	private final String JOB_WORK_DIR;
	
	private final static String ALIGNED_FASTA = "-aligned.fasta";
	private final static String INPUT_XML = ".xml";
	private final static String OUTPUT_TREES = "trees";
	private final static String RESULT_TREE = ".tree";
	private final static String GLM_SUFFIX = "_GLMedits";
	
	private final Logger log;
	private final ZooPhyMailer mailer;
	private final ZooPhyJob job;
	private Set<String> filesToCleanup;
	private File logFile;
	private Tailer tail = null;
	private Tailer rateTail = null;
	private Process beastProcess;
	private boolean wasKilled = false;
	private boolean isTest = false;
	private final int distinctLocations;
	private int mailUpdateCount = 0;
	
	public BeastRunner(ZooPhyJob job, ZooPhyMailer mailer, int distinctLocations) throws PipelineException {
		PropertyProvider provider = PropertyProvider.getInstance();
		JOB_LOG_DIR = provider.getProperty("job.logs.dir");
		BEAST_SCRIPTS_DIR = provider.getProperty("beast.scripts.dir");
		// SpreaD3 Settings
		SPREAD3 = provider.getProperty("spread3.jar");
		WORLD_GEOJSON = provider.getProperty("geojson.location");
		// Zoophy-Viz Settings
		ZOOPHY_VIZ = provider.getProperty("zoophyviz.dir");
		// FigTree Settings
		FIGTREE_TEMPLATE = System.getProperty("user.dir")+"/Templates/figtreeBlock.template";
		GLM_SCRIPT = provider.getProperty("glm.script");
		log = Logger.getLogger("BeastRunner"+job.getID());
		this.mailer = mailer;
		this.job = job;
		this.distinctLocations = distinctLocations;
		filesToCleanup = new LinkedHashSet<String>();
		JOB_WORK_DIR = System.getProperty("user.dir")+"/ZooPhyJobs/"+job.getID()+"/";
	}
	
	/**
	 * Runs the BEAST process
	 * @return resulting Tree File
	 * @throws PipelineException 
	 */
	public List<File> run() throws PipelineException {
		String resultingTree = null;
		FileHandler fileHandler = null;
		List<File> fileList = new ArrayList<File>();
		try {
			logFile = new File(JOB_LOG_DIR+job.getID()+".log");
			fileHandler = new FileHandler(JOB_LOG_DIR+job.getID()+".log", true);
			SimpleFormatter formatter = new SimpleFormatter();
	        fileHandler.setFormatter(formatter);
	        log.addHandler(fileHandler);
	        log.setUseParentHandlers(false);
			log.info("Starting the BEAST process...");
			runBeastGen(job.getID()+ALIGNED_FASTA, job.getID()+INPUT_XML, job.getXMLOptions());
			log.info("Adding location trait...");
			DiscreteTraitInserter traitInserter = new DiscreteTraitInserter(job, distinctLocations);
			traitInserter.addLocation();
			log.info("Location trait added.");
			if (job.isUsingGLM()) {
				log.info("Adding GLM Predictors...");
				runGLM();
				log.info("GLM Predictors added.");
			}
			else {
				log.info("Job is not using GLM.");
			}
			runBeast(job.getID());
			if (wasKilled || !PipelineManager.checkProcess(job.getID())) {
				throw new BeastException("Job was stopped!", "Job was stopped!");
			}
			if (job.isUsingGLM()) {
				resultingTree = runTreeAnnotator(job.getID()+"-aligned"+GLM_SUFFIX+"_states."+OUTPUT_TREES);
			}
			else {
				resultingTree = runTreeAnnotator(job.getID()+"-aligned."+OUTPUT_TREES);
			}
			File tree = new File(resultingTree);
			if (tree.exists()) {
				annotateTreeFile(resultingTree);
				File spread3 = runSpread();
				fileList.add(spread3);
				File spreadVideo = runZoophyViz();
				if (spreadVideo != null){
					fileList.add(spreadVideo);
				}
				log.info("BEAST process complete.");
			}
			else {
				log.log(Level.SEVERE, "TreeAnnotator did not proudce .tree file!");
				throw new BeastException("TreeAnnotator did not proudce .tree file!", "Tree Annotator Failed");
			}
			fileList.add(tree);
			return fileList;
		}
		catch (PipelineException pe) {
			log.log(Level.SEVERE, "BEAST process failed: "+pe.getMessage());
			throw pe;
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "BEAST process failed: "+e.getMessage());
			throw new BeastException("BEAST process failed: "+e.getMessage(), "BEAST Pipeline Failed");
		}
		finally {
			if (tail != null) {
				tail.stop();
			}
			if (rateTail != null) {
				rateTail.stop();
			}
			cleanupBeast();
			if (fileHandler != null) {
				fileHandler.close();
			}
		}
	}
	
	/**
	 * Generates an input.xml file to feed into BEAST
	 * @param fastaFile
	 * @param beastInput
	 * @param xmlParameters 
	 * @param beastSubstitutionModel 
	 * @throws BeastException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void runBeastGen(String fastaFile, String beastInput, XMLParameters xmlParameters) throws BeastException, IOException, InterruptedException {
		log.info("Finding BEASTGen template with parameters:" + xmlParameters.toString());
		String workingDir = JOB_WORK_DIR;
		File beastGenDir = new File(System.getProperty("user.dir")+"/BeastGen");
		filesToCleanup.add(JOB_WORK_DIR+fastaFile);
		//TODO: add/change templates programmatically by importing beastgen.jar's objects
		String template_name = "beastgen_SUBMODEL_CLOCKMODEL_PRIOR.template";
		String subModel = xmlParameters.getSubstitutionModel().toString();
		if(xmlParameters.isGamma()){
			subModel += "+G";
		}
		if(xmlParameters.isInvariantSites()){
			subModel += "+I";
		}
		template_name = template_name.replace("SUBMODEL", subModel);
		template_name = template_name.replace("CLOCKMODEL", xmlParameters.getClockModel().toString());
		template_name = template_name.replace("PRIOR", xmlParameters.getTreePrior().toString());
		log.info("Running BEASTGen Template:" + template_name);
		ProcessBuilder builder = new ProcessBuilder("java", "-jar", "beastgen.jar", "-date_order", "4", "-D", "chain_length="+xmlParameters.getChainLength().toString()+",log_every="+xmlParameters.getSubSampleRate().toString()+"" ,template_name, workingDir+fastaFile, workingDir+beastInput).directory(beastGenDir);
		builder.redirectOutput(Redirect.appendTo(logFile));
		builder.redirectError(Redirect.appendTo(logFile));
		log.info("Starting Process: "+builder.command().toString());
		Process beastGenProcess = builder.start();
		if (!isTest) {
			PipelineManager.setProcess(job.getID(), beastGenProcess);
		}
		beastGenProcess.waitFor();
		if (beastGenProcess.exitValue() != 0) {
			log.log(Level.SEVERE, "BeastGen failed! with code: "+beastGenProcess.exitValue());
			throw new BeastException("BeastGen failed! with code: "+beastGenProcess.exitValue(), "BeastGen Failed");
		}
		filesToCleanup.add(JOB_WORK_DIR+beastInput);
		log.info("BEAST input created.");
	}
	
	/**
	 * Adds GLM predictors to the BEAST XML input file
	 * @throws IOException 
	 * @throws InterruptedException
	 * @throws GLMException
	 */
	private void runGLM() throws IOException, InterruptedException, GLMException {
		log.info("Running BEAST_GLM...");
		final String GLM_PATH = JOB_WORK_DIR+job.getID()+"-"+"predictors.txt";
		final File PREDICTORS_FILE = new File(GLM_PATH);
		if (PREDICTORS_FILE.exists()) {
			final String BEAST_INPUT = JOB_WORK_DIR+job.getID()+INPUT_XML;
			ProcessBuilder builder = new ProcessBuilder("python3", GLM_SCRIPT, BEAST_INPUT, "states", "batch", PREDICTORS_FILE.getAbsolutePath()).directory(new File(JOB_WORK_DIR));
			builder.redirectOutput(Redirect.appendTo(logFile));
			builder.redirectError(Redirect.appendTo(logFile));
			log.info("Starting Process: "+builder.command().toString());
			Process beastGLMProcess = builder.start();
			if (!isTest) {
				PipelineManager.setProcess(job.getID(), beastGLMProcess);
			}
			OutputStream glmStream = beastGLMProcess.getOutputStream();
	        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(glmStream));
	        // Yes to creating Distance predictor from Latitude and Longitude
	        writer.write("y");
	        writer.write("\n");
	        writer.flush();
	        // No to raw Latitude predictor
	        writer.write("n");
	        writer.write("\n");
	        writer.flush();
	        // No to raw Longitude predictor
	        writer.write("n");
	        writer.write("\n");
	        writer.flush();
	        // Yes to confirm list of predictors
	        writer.write("y");
	        writer.write("\n");
	        writer.flush();
	        writer.close();
		    glmStream.close();
		    beastGLMProcess.waitFor();
			if (beastGLMProcess.exitValue() != 0) {
				log.log(Level.SEVERE, "BEAST GLM failed! with code: "+beastGLMProcess.exitValue());
				throw new GLMException("BEAST GLM failed! with code: "+beastGLMProcess.exitValue(), "BEAST_GLM failed!");
			}
			filesToCleanup.add(GLM_PATH);
			filesToCleanup.add(JOB_WORK_DIR+job.getID()+GLM_SUFFIX+INPUT_XML);
			filesToCleanup.add(JOB_WORK_DIR+job.getID()+"_distanceMatrix.txt");
			log.info("BEAST_GLM finished.");
		}
		else {
			log.log(Level.SEVERE, "Predictors file does not exist: "+GLM_PATH);
			throw new GLMException("No Predictors file found: "+PREDICTORS_FILE.getAbsolutePath(), "No GLM Predictors file found!");
		}
	}
	
	/**
	 * Runs BEAST on the input.xml file
	 * @param jobID
	 * @throws BeastException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void runBeast(String jobID) throws BeastException, IOException, InterruptedException {
		String input;
		if (job.isUsingGLM()) { 
			input = jobID+GLM_SUFFIX+INPUT_XML;
			filesToCleanup.add(JOB_WORK_DIR+jobID+"-aligned"+GLM_SUFFIX+"_states."+OUTPUT_TREES);
			filesToCleanup.add(JOB_WORK_DIR+jobID+"-aligned"+GLM_SUFFIX+"_states.log");
			filesToCleanup.add(JOB_WORK_DIR+jobID+"-aligned"+".ops");
		}
		else {
			input = jobID+INPUT_XML;
			filesToCleanup.add(JOB_WORK_DIR+jobID+"-aligned."+OUTPUT_TREES);
			filesToCleanup.add(JOB_WORK_DIR+jobID+"-aligned.log");
			filesToCleanup.add(JOB_WORK_DIR+jobID+"-aligned.ops");
			filesToCleanup.add(JOB_WORK_DIR+jobID+"-aligned.states.rates.log");
		}
		String beast = BEAST_SCRIPTS_DIR+"beast";
		log.info("Running BEAST...");
		File beastDir = new File(JOB_WORK_DIR);
		ProcessBuilder builder;
		builder = new ProcessBuilder(beast, JOB_WORK_DIR + input).directory(beastDir);
		builder.redirectOutput(Redirect.appendTo(logFile));
		builder.redirectError(Redirect.appendTo(logFile));
		BeastTailerListener listener = new BeastTailerListener();
		tail = new Tailer(logFile, listener);
		log.info("Starting Process: "+builder.command().toString());
		Process beastProcess = builder.start();
		PipelineManager.setProcess(job.getID(), beastProcess);
		tail.run();
		beastProcess.waitFor();
		tail.stop();
		if (beastProcess.exitValue() != 0) {
			tail.stop();
			log.log(Level.SEVERE, "BEAST failed! with code: "+beastProcess.exitValue());
			throw new BeastException("BEAST failed! with code: "+beastProcess.exitValue(), "BEAST Failed");
		}
		if (wasKilled) {
			return;
		}
		String outputPath;
		if (job.isUsingGLM()) {
			outputPath = JOB_WORK_DIR+jobID+"-aligned"+GLM_SUFFIX+"_states."+OUTPUT_TREES;
		}
		else {
			outputPath = JOB_WORK_DIR+jobID+"-aligned."+OUTPUT_TREES;
		}
		File beastOutput = new File(outputPath);
		if (!beastOutput.exists() || scanForBeastError()) {
			log.log(Level.SEVERE, "BEAST did not produce output! Trying it in always scaling mode...");
			builder = new ProcessBuilder(beast, "-beagle_scaling", "always", "-overwrite", JOB_WORK_DIR + input).directory(beastDir);
			builder.redirectOutput(Redirect.appendTo(logFile));
			builder.redirectError(Redirect.appendTo(logFile));
			log.info("Starting Process: "+builder.command().toString());
			Process beastRerunProcess = builder.start();
			PipelineManager.setProcess(job.getID(), beastRerunProcess);
			tail.run();
			beastRerunProcess.waitFor();
			tail.stop();
			if (beastRerunProcess.exitValue() != 0) {
				tail.stop();
				log.log(Level.SEVERE, "Always-scaling BEAST failed! with code: "+beastProcess.exitValue());
				throw new BeastException("Always-scaling BEAST failed! with code: "+beastProcess.exitValue(), "BEAST Failed");
			}
			beastOutput = new File(outputPath);
			if (!beastOutput.exists()) {
				log.log(Level.SEVERE, "Always-scaling BEAST did not produce output!");
				throw new BeastException("Always-scaling BEAST did not produce output!", "BEAST Failed");
			}
		}
		log.info("BEAST finished.");
	}

	/**
	 * Runs the Tree Annotator to generate the final .tree file
	 * @param trees
	 * @return File path to resulting Tree File
	 * @throws BeastException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private String runTreeAnnotator(String trees) throws BeastException, IOException, InterruptedException {
		String tree;
		if (job.isUsingGLM()) {
			tree = trees.substring(0, trees.indexOf("-aligned"+GLM_SUFFIX)) + RESULT_TREE;
		}
		else {
			tree = trees.substring(0, trees.indexOf("-aligned")) + RESULT_TREE;
		}
		String treeannotator = BEAST_SCRIPTS_DIR+"treeannotator";
		log.info("Running Tree Annotator...");
		ProcessBuilder builder = new ProcessBuilder(treeannotator,"-burnin", "1000", JOB_WORK_DIR+trees, JOB_WORK_DIR+tree);
		builder.redirectOutput(Redirect.appendTo(logFile));
		builder.redirectError(Redirect.appendTo(logFile));
		log.info("Starting Process: "+builder.command().toString());
		Process treeAnnotatorProcess = builder.start();
		PipelineManager.setProcess(job.getID(), treeAnnotatorProcess);
		treeAnnotatorProcess.waitFor();
		if (treeAnnotatorProcess.exitValue() != 0) {
			log.log(Level.SEVERE, "Tree Annotator failed! with code: "+treeAnnotatorProcess.exitValue());
			throw new BeastException("Tree Annotator failed! with code: "+treeAnnotatorProcess.exitValue(), "Tree Annotator Failed");
		}
		log.info("Tree Annotator finished.");
		return JOB_WORK_DIR+tree;
	}
	
	/**
	 * Appends a FigTree block to the .tree file
	 * @param treeFile
	 * @throws BeastException
	 */
	private void annotateTreeFile(String treeFile) throws BeastException {
		try {
			String ageOffset = "scale.offsetAge=";
			String youngestAge = findYougestAge(treeFile);
			FileWriter filewRiter = new FileWriter(treeFile, true);
			BufferedWriter bufferWriter = new BufferedWriter(filewRiter);
		    PrintWriter printer = new PrintWriter(bufferWriter);
		    Scanner scan = new Scanner(new File(FIGTREE_TEMPLATE));
		    while (scan.hasNext()) {
		    	String line = scan.nextLine();
		    	if (line.contains(ageOffset)) {
		    		line = line.substring(0,line.indexOf(ageOffset)+ageOffset.length())+youngestAge+";";
		    	}
		    	printer.println(line);
		    }
		    scan.close();
		    printer.close();
		    bufferWriter.close();
		    filewRiter.close();
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "ERROR ADDING FIGTREE BLOCK: "+e.getMessage());
			throw new BeastException("ERROR ADDING FIGTREE BLOCK: "+e.getMessage(), "BEAST Pipeline Failed");
		}
	}
	
	/**
	 * Runs zoophy-viz to generate data visualization video file using python 
	 * @return File path to resulting video mp4 file
	 * @throws BeastException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private File runZoophyViz() throws BeastException, InterruptedException, IOException {
		File spreadVideo = null;
		String workingDir = JOB_WORK_DIR+job.getID();
		log.info("Running the python zoophy-viz generator...");
		String coordinatesFile = workingDir+"-coords.txt";
		String treeFile = workingDir+".tree";
		File zoophyVizDirectory = new File(ZOOPHY_VIZ);
		if (!zoophyVizDirectory.exists()) {
			log.log(Level.SEVERE, "Invalid absolute path to zoophy-viz repository given: "+ZOOPHY_VIZ);
			throw new BeastException("Invalid absolute path to zoophy-viz repository given!"+ZOOPHY_VIZ, "zoophy-viz failed");
		}
		ProcessBuilder builder = new ProcessBuilder("./gen_pgmt_spread.sh", treeFile, coordinatesFile, workingDir).directory(zoophyVizDirectory);
		builder.redirectOutput(Redirect.appendTo(logFile));
		builder.redirectError(Redirect.appendTo(logFile));
		log.info("Starting Process: "+builder.command().toString());
		Process zoophyVizProcess = builder.start();
		PipelineManager.setProcess(job.getID(), zoophyVizProcess);
		zoophyVizProcess.waitFor();
		if (zoophyVizProcess.exitValue() != 0) {
			log.log(Level.SEVERE, "zoophy-viz generation failed! with code: "+zoophyVizProcess.exitValue());
			// throw new BeastException("zoophy-viz generation failed! with code: "+zoophyVizProcess.exitValue(), "zoophy-viz failed");
		} else {
			spreadVideo = new File(workingDir + "/spread.mp4");
			if (!spreadVideo.exists()) {
				spreadVideo = null;
			}
		}
		log.info("zoophy-viz finished.");
		return spreadVideo;
	}

	/**
	 * Runs SpreaD3 to generate data visualization files 
	 * @throws BeastException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private File runSpread() throws BeastException, InterruptedException, IOException {
		String workingDir = JOB_WORK_DIR+job.getID();
		log.info("Running SpreaD3 generator...");
		String coordinatesFile = workingDir+"-coords.txt";
		String treeFile = workingDir+".tree";
		String youngestDate = findYougestAge(treeFile);
		String spreadFile = workingDir+"-spread3.json";
		File spreadDirectory = new File(SPREAD3);
		if (spreadDirectory.exists() && spreadDirectory.isFile() && spreadDirectory.getParent() != null) {
			spreadDirectory = new File(spreadDirectory.getParent());
		}
		else {
			log.log(Level.SEVERE, "Invalid absolute path to SpreaD3 given: "+SPREAD3);
		}
		ProcessBuilder builder = new ProcessBuilder("java","-jar",SPREAD3,"-parse","-locations",coordinatesFile,"-header","false","-tree",treeFile,"-locationTrait","states","-intervals","10","-mrsd",youngestDate,"-geojson",WORLD_GEOJSON,"-output",spreadFile).directory(spreadDirectory);
		builder.redirectOutput(Redirect.appendTo(logFile));
		builder.redirectError(Redirect.appendTo(logFile));
		log.info("Starting Process: "+builder.command().toString());
		Process spreadGenerationProcess = builder.start();
		PipelineManager.setProcess(job.getID(), spreadGenerationProcess);
		spreadGenerationProcess.waitFor();
		if (spreadGenerationProcess.exitValue() != 0) {
			log.log(Level.SEVERE, "SpreaD3 generation failed! with code: "+spreadGenerationProcess.exitValue());
		}
		log.info("SpreaD3 finished.");
		return new File(spreadFile);
	}

	/**
	 * Deletes unwanted files after the BEAST process is finished
	 */
	private void cleanupBeast() {
		// This function is disabled to access the job files in case of an error
	/*	log.info("Cleaning up BEAST...");
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
			log.log(Level.SEVERE, "BEAST cleanup failed: "+e.getMessage());
		}*/
	}
	
	/**
	 * Finds the youngest Sequence date in the .tree file
	 * @param treeFile
	 * @return youngest Sequence date in the .tree file in decimal format
	 * @throws BeastException
	 */
	private String findYougestAge(String treeFile) throws BeastException {
		String youngestAge = "1996.0861";
		try {
			double minAge = 1920.0;
			double currAge = 0;
			Scanner scan = new Scanner(new File(treeFile));
			String line = scan.nextLine();
			while (!line.contains("Taxlabels")) {
				line = scan.nextLine();
			}
			line = scan.nextLine();
			while (line.contains("_")) {
				currAge = Double.parseDouble(line.split("_")[3]);
				if (currAge > minAge) {
					minAge = currAge;
					youngestAge = line.split("_")[3].trim();
				}
				line = scan.nextLine();
			}
			scan.close();
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "ERROR SETTING FIGTREE START DATE: "+e.getMessage());
			throw new BeastException("ERROR SETTING FIGTREE START DATE: "+e.getMessage() , "BEAST Pipeline Failed");
		}
		return youngestAge;
	}
	
	/**
	 * Sends a time estimate to the user
	 * @param finishTime
	 * @param finalUpdate
	 */
	private void sendUpdate(String finishTime, boolean finalUpdate) {
		if (finalUpdate) {
			tail.stop();
		}
		try {
			if (finalUpdate || checkRateMatrix()) {
				//reason for count: handler reads a couple more lines and sends update mail even after tail.stop()
				if(mailUpdateCount < 2) {
					mailer.sendUpdateEmail(finishTime, finalUpdate);
					mailUpdateCount ++;
				}
			}
			else {
				killBeast("Rate Matrix Error. Try reducing discrete states.");
			}
		}
		catch (Exception e) {
			if (rateTail != null) {
				rateTail.stop();
			}
			tail.stop();
			log.log(Level.SEVERE, "Error sending email: "+e.getMessage());
		}
	}
	
	/**
	 * Checks the jog log for a BEAST runtime error that may have still reported an exit code of 0
	 * @return True if the log contains a RuntimeException, False otherwise
	 * @throws FileNotFoundException
	 */
	private boolean scanForBeastError() throws FileNotFoundException {
		Scanner scan = new Scanner(logFile);
		String line;
		while (scan.hasNext()) {
			line = scan.nextLine();
			if (line.contains("java.lang.RuntimeException: An error was encounted. Terminating BEAST")) {
				scan.close();
				return true;
			}
		}
		scan.close();
		return false;
	}
	
	/**
	 * Checks the rates log file for obvious errors
	 * @return True if the rate matrix file is not obviously invalid, False otherwise
	 */
	private boolean checkRateMatrix() {
		try {
			String rateLogPath;
			if (job.isUsingGLM()) {
				rateLogPath = JOB_WORK_DIR+job.getID()+GLM_SUFFIX+"_states.model.log";
			}
			else {
				rateLogPath = JOB_WORK_DIR+job.getID()+"-aligned.states.rates.log";
			}
			File rateLog = new File(rateLogPath);
			if (rateLog.exists()) {
				RateTailerListener rateListener = new RateTailerListener();
				rateTail = new Tailer(rateLog, rateListener);
				System.out.print("Starting rateTailer on "+rateLog.getAbsolutePath());
				rateTail.run();
			}
			else {
				throw new Exception("Rate Log does not exist: "+rateLogPath);
			}
			return true;
		}
		catch (Exception e) {
			if (rateTail != null) {
				rateTail.stop();
			}
			System.err.println("Error checking rate matrix: "+e.getMessage());
			return false;
		}
	}
	
	/**
	 * Kill the running Beast job
	 * @param reason - Reason for stopping the job
	 */
	private void killBeast(String reason) {
		if (tail != null) {
			tail.stop();
		}
		if (rateTail != null) {
			rateTail.stop();
		}
		mailer.sendFailureEmail(reason);
		wasKilled = true;
		beastProcess.destroy();
		PipelineManager.removeProcess(job.getID());
	}
	
	/**
	 * Tails the job log to screen BEAST output
	 * @author devdemetri
	 */
	private class BeastTailerListener extends TailerListenerAdapter {
	  boolean reached = false;
	  boolean finalUpdate = false;
	  
	  public void handle(String line) {
		  if (line != null && !(line.trim().isEmpty() || line.contains("INFO:") || line.contains("usa.ac.asu.dbi.diego.viralcontamination3"))) {
			  if ((line.contains("hours/million states") || line.contains("minutes/million states")) && (reachedCheck(line.trim()) || reached)) {
				  if (!PipelineManager.checkProcess(job.getID())) {
					  tail.stop();
					  killBeast("Process was already terminated.");
				  }
				  else {
					System.out.println("Tailer Reading: "+line);
					  reached = true;
					  try {
						  String[] beastColumns = line.split("\t");
						  if (beastColumns.length > 0) {
							  String progressRate = beastColumns[beastColumns.length-1].trim();
							  int estimatedHoursToGo;
							  double hoursPerMillion = Double.parseDouble(progressRate.substring(0, progressRate.indexOf(' ')));
							  // if reporting is in minutes, convert to hours 
							  if (progressRate.contains("minutes")){
								hoursPerMillion = hoursPerMillion/60;
							  }
							  double millionsInJob = Math.ceil(job.getXMLOptions().getChainLength() / 1000000);
							  if (finalUpdate) {
								  estimatedHoursToGo = (int) Math.ceil(hoursPerMillion*(millionsInJob*0.5));
								  tail.stop();
							  }
							  else {
								  estimatedHoursToGo = (int) Math.ceil(hoursPerMillion*(millionsInJob*0.9));
							  }
					  		  Date currentDate = new Date();
					  		  Calendar calendar = Calendar.getInstance();
					  		  calendar.setTime(currentDate);
					  		  calendar.add(Calendar.HOUR, estimatedHoursToGo);
					  		  String finishTime = calendar.getTime().toString();
					  		  sendUpdate(finishTime, finalUpdate);
					  		  reached = false;
					  		  finalUpdate = true;
					  	  }
					  }
					  catch (Exception e) {
						  log.log(Level.WARNING, "Failed to extract Beast time: "+e.getMessage());
					  }
				  }
			  }
			  else if (line.contains("java.lang.RuntimeException")) {
				  tail.stop();
			  }
		  }
	  }
	  
	  /**
	   * @return True if tailer checkpoint reached, False otherwise
	   */
	  private boolean reachedCheck(String line) {
		  int checkpoint;
		  if (finalUpdate) {
			  checkpoint = job.getXMLOptions().getChainLength() / 2;
		  }
		  else {
			  checkpoint = job.getXMLOptions().getChainLength() / 10;
		  }
		  try {
			  String[] beastColumns = line.split("\t");
			  int sample = Integer.parseInt(beastColumns[0].trim());
			  if (sample >= checkpoint) {
				  return true;
			  }
			  else {
				  return false;
			  }
		  }
		  catch (Exception e) {
			  log.warning("Error checking for time checkpoint: "+e.getMessage());
			  return false;
		  }
	  }
	}
	
	/**
	 * Listener for RateTailer
	 * @author devdemetri
	 */
	private class RateTailerListener extends TailerListenerAdapter {
		
		public void handle(String line) {
			boolean isFailing = true;
			final double standard = 1.0;
			if (line != null && reachedCheck(line.trim())) {
				rateTail.stop();
				if (!PipelineManager.checkProcess(job.getID())) {
					  killBeast("Process was already terminated.");
				}
				else {
					try {
						String[] row = line.trim().split("\t");
						for (int i = 1; i < row.length; i++) {
							double col = Double.parseDouble(row[i].trim());
							if (col != standard) {
								isFailing = false;
							}
						}
						if (isFailing) {
							killBeast("Rate Matrix Error. Try reducing discrete states.");
						}
					}
					catch (Exception e) {
						rateTail.stop();
						System.err.println("ERROR checking rate matrix file: "+e.getMessage());
						throw e;
					}
				}
			}
		}
		
		/**
		   * @return True if tailer checkpoint reached, False otherwise
		   */
		  private boolean reachedCheck(String line) {
			  int checkpoint = job.getXMLOptions().getChainLength() / 500;
			  try {
				  String[] beastColumns = line.split("\t");
				  int sample = Integer.parseInt(beastColumns[0].trim());
				  if (sample >= checkpoint) {
					  return true;
				  }
				  else {
					  return false;
				  }
			  }
			  catch (Exception e) {
				  log.warning("Error checking for initial checkpoint: "+e.getMessage());
				  return false;
			  }
		  }
		
	}

	/**
	 * Test the given ZooPhy job in the quick early stages
	 * @throws PipelineException
	 */
	public void test() throws PipelineException {
		isTest = true;
		FileHandler fileHandler = null;
		try {
			logFile = new File(JOB_LOG_DIR+job.getID()+".log");
			fileHandler = new FileHandler(JOB_LOG_DIR+job.getID()+".log", true);
			SimpleFormatter formatter = new SimpleFormatter();
	        fileHandler.setFormatter(formatter);
	        log.addHandler(fileHandler);
	        log.setUseParentHandlers(false);
			log.info("Starting the BEAST test process...");
			runBeastGen(job.getID()+ALIGNED_FASTA, job.getID()+INPUT_XML, job.getXMLOptions());
			log.info("Adding location trait...");
			DiscreteTraitInserter traitInserter = new DiscreteTraitInserter(job, distinctLocations);
			traitInserter.addLocation();
			log.info("Location trait added.");
			if (job.isUsingGLM()) {
				log.info("Adding GLM Predictors...");
				runGLM();
				log.info("GLM Predictors added.");
				String predictorData = JOB_WORK_DIR+job.getID()+"_predictorNames.txt";
				filesToCleanup.add(predictorData);
			}
			else {
				log.info("Job is not using GLM.");
			}
		}
		catch (PipelineException pe) {
			log.log(Level.SEVERE, "BEAST test process failed: "+pe.getMessage());
			throw pe;
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "BEAST test process failed: "+e.getMessage());
			throw new BeastException("BEAST test process failed: "+e.getMessage(), "BEAST Pipeline Failed");
		}
		finally {
			cleanupBeast();
			if (fileHandler != null) {
				fileHandler.close();
			}
		}
	}
	
}
