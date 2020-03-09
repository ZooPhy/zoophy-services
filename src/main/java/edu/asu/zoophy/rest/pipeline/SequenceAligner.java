package edu.asu.zoophy.rest.pipeline;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.asu.zoophy.rest.custom.FastaRecord;
import edu.asu.zoophy.rest.database.DaoException;
import edu.asu.zoophy.rest.database.GenBankRecordNotFoundException;
import edu.asu.zoophy.rest.database.ZooPhyDAO;
import edu.asu.zoophy.rest.genbank.ExcludedRecords;
import edu.asu.zoophy.rest.genbank.GenBankRecord;
import edu.asu.zoophy.rest.genbank.InvalidRecords;
import edu.asu.zoophy.rest.genbank.JobAccessions;
import edu.asu.zoophy.rest.genbank.JobRecords;
import edu.asu.zoophy.rest.genbank.Location;
import edu.asu.zoophy.rest.index.LuceneHierarchySearcher;
import edu.asu.zoophy.rest.pipeline.glm.GLMException;
import edu.asu.zoophy.rest.pipeline.glm.PredictorGenerator;
import edu.asu.zoophy.rest.pipeline.utils.DisjoinerException;
import edu.asu.zoophy.rest.pipeline.utils.GeonameDisjoiner;
import edu.asu.zoophy.rest.pipeline.utils.Normalizer;
import edu.asu.zoophy.rest.pipeline.utils.NormalizerException;
import edu.asu.zoophy.rest.security.SecurityHelper;

/**
 * Responsible for aligning sequences into FASTA format
 * @author devdemetri, kbhangal
 */
public class SequenceAligner {

	private final String JOB_LOG_DIR;
	private final ZooPhyJob job;
	private final ZooPhyDAO dao;
	private final LuceneHierarchySearcher hierarchyIndexSearcher;
	private final Logger log;
	private File logFile;
	private Set<String> uniqueGeonames;
	private Set<Location> uniqueLocations;
	private Map<String,String> geonameCoordinates;
	private int startYear = 3000;
	private int endYear = 1000;
	private Map<String, Integer> occurrences = null;
	private int DEFAULT_POPSIZE = 10;
	private final String JOB_WORK_DIR;
	
	/**
	 * Constructor for regular ZooPhy Pipeline usage
	 * @param job - ZooPhyJob for Predictor data
	 * @param dao - ZooPhyDAO for SQL operations
	 * @param hierarchyIndexSearcher - LuceneHierarchySearcher for index operations
	 * @throws PipelineException
	 */
	public SequenceAligner(ZooPhyJob job, ZooPhyDAO dao, LuceneHierarchySearcher hierarchyIndexSearcher) throws PipelineException {
		this.dao = dao;
		this.hierarchyIndexSearcher = hierarchyIndexSearcher;
		PropertyProvider provider = PropertyProvider.getInstance();
		JOB_LOG_DIR = provider.getProperty("job.logs.dir");
		JOB_WORK_DIR = System.getProperty("user.dir")+"/ZooPhyJobs/"+job.getID()+"/";
		log = Logger.getLogger("SequenceAligner"+job.getID());
		uniqueGeonames = new LinkedHashSet<String>();
		uniqueLocations = new LinkedHashSet<>();
		geonameCoordinates = new HashMap<String,String>();
		if (job.isUsingGLM() && !job.isUsingCustomPredictors()) {
			occurrences = new HashMap<String, Integer>();
		}
		this.job = job;
		new File(JOB_WORK_DIR).mkdirs();			//create a new work directory for the job.
	}
	
	/**
	 * NOTE: Only use this constructor for generating downloadable FASTA, not for ZooPhy Jobs
	 * @param dao - ZooPhyDAO for SQL operations
	 * @param hierarchyIndexSearcher - LuceneHierarchySearcher for index operations
	 */
	public SequenceAligner(ZooPhyDAO dao, LuceneHierarchySearcher hierarchyIndexSearcher) {
		log = Logger.getLogger("SequenceAligner");
		this.dao = dao;
		this.hierarchyIndexSearcher = hierarchyIndexSearcher;
		JOB_LOG_DIR = null;
		JOB_WORK_DIR = null;
		job = null;
	}
	
	/**
	 * Sequence Alignment Pipeline for genbank and Fasta records that runs:
	 * 1) Geoname Disjoiner
	 * 2) GLM Predictor Generator (only if job is using GLM)
	 * 3) FASTA formatting of raw sequences
	 * 4) MAFFT sequence alignment
	 * @param accessions - genbank record sequences to be included in FASTA
	 * @param fastaRecs - fasta record sequences to be included in FASTA
	 * @param isTest - True iff actual alignment can be skipped for a test run
	 * @return Final List of Records to be used in the Job
	 * @throws PipelineException
	 */
	public JobAccessions align(List<String> accessions, List<FastaRecord> fastaRecs, boolean isTest) throws PipelineException {
		FileHandler fileHandler = null;
		String rawFasta= "";
		JobAccessions jobAccessions = new JobAccessions();
		Set<String> usedAccessions = new HashSet<String>();
		try {
			JobRecords jobRecords = null;
			List<GenBankRecord> recs =new LinkedList<>();
			logFile = new File(JOB_LOG_DIR+job.getID()+".log");
			fileHandler = new FileHandler(JOB_LOG_DIR+job.getID()+".log", true);
			SimpleFormatter formatter = new SimpleFormatter();  
	        fileHandler.setFormatter(formatter);
	        log.addHandler(fileHandler);
	        log.setUseParentHandlers(false);
			log.info("Starting Mafft Job: "+job.getID());
			jobRecords = loadSequences(accessions, fastaRecs, true, (job.isUsingGLM() && !job.isUsingCustomPredictors()));
			recs = jobRecords.getValidRecordList();
			
			jobAccessions.setDistinctLocations(jobRecords.getDistinctLocations());
			jobAccessions.setInvalidRecordList(jobRecords.getInvalidRecordList());
			
			List<GenBankRecord> recsGenbank = new LinkedList<>();
			List<String> fastaID = new LinkedList<>();
			Iterator<FastaRecord> recordIter = fastaRecs.listIterator();
			for(GenBankRecord rec : recs) {
				if(accessions.contains(rec.getAccession())) {
					recsGenbank.add(rec);
				}else {
					fastaID.add(rec.getAccession());
				}
				usedAccessions.add(rec.getAccession());
			}
			while (recordIter.hasNext()) {
				FastaRecord fastaRec = recordIter.next();
				if(!fastaID.contains(fastaRec.getAccession())){
					recordIter.remove();
				}
			}
			jobAccessions.setValidAccessions(usedAccessions);
			if(recsGenbank.size()>0) {
				log.info("After screening accession job includes: "+recsGenbank.size()+" records.");
				rawFasta = fastaFormat(recsGenbank, (job.isUsingGLM() && !job.isUsingCustomPredictors()));
			}
			if(fastaRecs.size()>0) {
				log.info("After screening fasta job includes: "+fastaRecs.size()+" records.");
				rawFasta += convertCustomRecordToFasta(fastaRecs, (job.isUsingGLM() && !job.isUsingCustomPredictors()));
				
			}
			createCoordinatesFile();
			if (job.isUsingGLM()) {
				createGLMFile(job.isUsingGLM() && !job.isUsingCustomPredictors());
			}
			if (isTest) {
				fakeMafft(rawFasta);
			}
			else {
				runMafft(rawFasta);
			}
			log.info("Mafft Job: "+job.getID()+" has finished.");
			log.info("Deleting raw fasta...");
			try {
				Path path = Paths.get(System.getProperty("user.dir")+"/ZooPhyJobs/"+job.getID()+"/"+job.getID()+"-raw.fasta");
				Files.delete(path);
			}
			catch (IOException e) {
				log.log(Level.SEVERE, "ERROR! could not delete raw fasta: "+e.getMessage());
				throw e;
			}
			log.info("Mafft process complete");
			fileHandler.close();

			return jobAccessions;
		}
		catch (PipelineException pe) {
			log.log(Level.SEVERE, "ERROR! Mafft process failed: "+pe.getMessage());
			throw pe;
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "ERROR! Mafft process failed: "+e.getMessage());
			throw new AlignerException(e.getMessage(), "ERROR Mafft process failed.");
		}
		finally {
			if (fileHandler != null) {
				fileHandler.close();
			}
		}
	}
	
	/**
	 * Generates the GLM predictors batch file 
	 * @param usingDefault 
	 * @throws GLMException 
	 */
	private void createGLMFile(boolean usingDefault) throws GLMException {
		String glmPath = System.getProperty("user.dir")+"/ZooPhyJobs/"+job.getID()+"/"+job.getID()+"-"+"predictors.txt";
		if (usingDefault) {
			PredictorGenerator generator = new PredictorGenerator(glmPath, startYear, endYear, uniqueGeonames, dao, hierarchyIndexSearcher);
			generator.generateDefaultPredictorsFile(occurrences, uniqueLocations);
		}
		else {
			PredictorGenerator.writeCustomPredictorsFile(glmPath, job.getPredictors());
		}
	}

	/**
	 * Creates the coordinates file needed for SpreaD3
	 */
	private void createCoordinatesFile() {
		StringBuilder coordinates = new StringBuilder();
		for (String location : uniqueGeonames) {
			coordinates.append(geonameCoordinates.get(location));
			coordinates.append("\n");
		}
		coordinates.trimToSize();
		String dir = System.getProperty("user.dir")+"/ZooPhyJobs/"+job.getID()+"/"+job.getID()+"-";
		PrintWriter coordinateWriter = null;
		try {
			coordinateWriter = new PrintWriter(dir+"coords.txt");
			coordinateWriter.write(coordinates.toString());
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Error setting up coords.txt: "+e.getMessage());
		}
		finally {
			if (coordinateWriter != null) {
				coordinateWriter.close();
			}
		}
	}
	

	/**
	 * Loads GenBank and Fasta Records that contain the desired sequences. This may also include running the GeonameDisjoiner 
	 * @param accessions - genbank records to load
	 * @param fastaRecords - Fasta records to load
	 * @param isDisjoint - whether to run GeonameDisjoiner on the records or not
	 * @return list of GenBankRecords ready for MAFFT 
	 * @throws GenBankRecordNotFoundException
	 * @throws DaoException
	 * @throws PipelineException
	 */
	public JobRecords loadSequences(List<String> accessions, List<FastaRecord> fastaRecords, boolean isDisjoint, boolean isUsingDefaultGLM) throws GenBankRecordNotFoundException, DaoException, PipelineException {
		log.info("Loading records for Mafft...");
		List<GenBankRecord> records = new LinkedList<GenBankRecord>();
		List<ExcludedRecords> missingDateRecords = new LinkedList<>();
		List<ExcludedRecords> unknowndateFormatRecords = new LinkedList<>();
		
		for (String accession : accessions) {
			GenBankRecord record = dao.retrieveFullRecord(accession);
			try {
				if(record.getSequence().getCollectionDate() != null  && !getFastaDate(record.getSequence().getCollectionDate()).equalsIgnoreCase("unknown")) {
					if (record != null && record.getGeonameLocation() != null) { 
						records.add(record);
					}
				} else {
					missingDateRecords.add(new ExcludedRecords(accession, null));
					log.log(Level.SEVERE, "ERROR! Record: "+accession+" removed. No Collection date found!");
				}
			}
			catch (Exception e) {
				unknowndateFormatRecords.add(new ExcludedRecords(accession, null));
				log.log(Level.SEVERE, "ERROR! Issue Adding Record: "+accession+" : "+e.getMessage());
			}
		}
		if(fastaRecords!=null) {
			for(FastaRecord fastaRec : fastaRecords) {
				GenBankRecord record = new GenBankRecord();
				record.setAccession(fastaRec.getAccession());
				record.setGeonameLocation(fastaRec.getGeonameLocation());
				records.add(record);
			}
		}
		log.info("Records loaded.");
		if (records.size() < 2) {
			String userErr = "Too few valid records with necessary information (e.g. Date, Location) (need at least 2): " + records.size();
			throw new DisjoinerException("Too few valid records: "+records.size(),userErr);
		}
		if (isDisjoint) {
			GeonameDisjoiner disjoiner  = new GeonameDisjoiner(hierarchyIndexSearcher);
			if (isUsingDefaultGLM && false) {
				JobRecords jobRecords = disjoiner.disjoinRecordsToStates(records);
				if(missingDateRecords.size()>0) {
					jobRecords.getInvalidRecordList().add(new InvalidRecords(missingDateRecords, "Missing Date Information"));
				}
				if(unknowndateFormatRecords.size()>0) {
					jobRecords.getInvalidRecordList().add(new InvalidRecords(unknowndateFormatRecords, "Unknown Date Format"));
				}
				return jobRecords;
			}
			else {
				JobRecords jobRecords = disjoiner.disjoinRecords(records);
				if(missingDateRecords.size()>0) {
					jobRecords.getInvalidRecordList().add(new InvalidRecords(missingDateRecords, "Missing Date Information"));
				}
				if(unknowndateFormatRecords.size()>0) {
					jobRecords.getInvalidRecordList().add(new InvalidRecords(unknowndateFormatRecords, "Unknown Date Format"));
				}
				return jobRecords;
			}
		}
		else {
			List<ExcludedRecords> missingLocationRecords = new LinkedList<>();
			List<InvalidRecords> invalidRecords = new LinkedList<>();
			
			for (int i = 0; i < records.size(); i++) {
				GenBankRecord record = records.get(i);
				if (record.getGeonameLocation() == null || Normalizer.normalizeLocation(record.getGeonameLocation()).equalsIgnoreCase("unknown")) {
					missingLocationRecords.add(new ExcludedRecords(record.getAccession(), null));
					records.remove(i);
				}
			}
			if(!missingLocationRecords.isEmpty()) {
				invalidRecords.add(new InvalidRecords(missingLocationRecords,"Missing Location Information"));
			}
			if(!missingDateRecords.isEmpty()) {
				invalidRecords.add(new InvalidRecords(missingDateRecords,"Missing Date Information"));
			}
			if(!unknowndateFormatRecords.isEmpty()) {
				invalidRecords.add(new InvalidRecords(unknowndateFormatRecords,"Unknown Date Format"));
			}
			JobRecords jobRecords = new JobRecords(records, invalidRecords, DEFAULT_POPSIZE);
			return jobRecords;
		}
	}

	/**
	 * Runs MAFFT to Align Sequences
	 * @param rawFasta - String of raw fasta formatted sequences
	 * @return file path to MAFFT aligned .fasta file
	 * @throws AlignerException 
	 */
	private String runMafft(String rawFasta) throws AlignerException {
		log.info("Setting up Mafft for job: "+job.getID());
		String dir = System.getProperty("user.dir")+"/ZooPhyJobs/"+job.getID()+"/"+job.getID()+"-";
		String rawFilePath = dir + "raw.fasta";
		String alignedFilePath = dir+"aligned.fasta";
		try {
			PrintWriter printer = new PrintWriter(rawFilePath);
			printer.write(rawFasta);
			printer.close();
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Error setting up raw.fasta: "+e.getMessage());
		}
		File outFile = new File(alignedFilePath);
		try {
			ProcessBuilder builder = new ProcessBuilder("mafft", "--auto", rawFilePath);
			builder.redirectOutput(Redirect.appendTo(outFile));
			builder.redirectError(Redirect.appendTo(logFile));
			log.info("Running Mafft...");
			Process mafftProcess = builder.start();
			PipelineManager.setProcess(job.getID(), mafftProcess);
			mafftProcess.waitFor();
			if (mafftProcess.exitValue() != 0) {
				log.log(Level.SEVERE, "Mafft failed! with code: "+mafftProcess.exitValue());
				throw new Exception("Mafft failed! with code: "+mafftProcess.exitValue());
			}
			log.info("Mafft finished.");
		} 
		catch (Exception e) {
			log.log(Level.SEVERE, "Error running mafft: "+e.getMessage());
			throw new AlignerException("Error running mafft: "+e.getMessage(), null);
		}
		return alignedFilePath;
	}
	
	/**
	 * FOR TEST USE ONLY
	 * To save time, the raw fasta is copied to an aligned fasta file instead of actually funning Mafft
	 * Useful for quickly validating job parameters before starting job
	 * @param rawFasta
	 * @return file path to fake aligned .fasta file that is really just a copy of the raw .fasta file
	 */
	private String fakeMafft(String rawFasta) {
		log.info("Faking Mafft for job: "+job.getID());
		String dir = System.getProperty("user.dir")+"/ZooPhyJobs/"+job.getID()+"/"+job.getID()+"-";
		String rawFilePath = dir + "raw.fasta";
		String alignedFilePath = dir+"aligned.fasta";
		try {
			PrintWriter printer = new PrintWriter(rawFilePath);
			printer.write(rawFasta);
			printer.close();
			printer = new PrintWriter(alignedFilePath);
			printer.write(rawFasta);
			printer.close();
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Error faking aligned fasta: "+e.getMessage());
		}
		log.info("Fake aligned fasta file created.");
		return alignedFilePath;
	}

	/**
	 * Combines the records' sequences into a FASTA formatted String
	 * @param records List of full GenBankRecords
	 * @return String FASTA formatted sequences
	 * @throws AlignerException 
	 * @throws Exception 
	 */
	private String fastaFormat(List<GenBankRecord> records, boolean isUsingDefaultGLM) throws AlignerException {
		log.info("Starting Fasta formatting");
		StringBuilder builder = new StringBuilder();
		StringBuilder tempBuilder;
		for (GenBankRecord record : records) {
			try {
				tempBuilder = new StringBuilder();
				tempBuilder.append(">");
				tempBuilder.append(record.getAccession());
				tempBuilder.append("_");
				tempBuilder.append(record.getSequence().getTaxID());
				tempBuilder.append("_");
				tempBuilder.append(record.getHost().getTaxon());
				tempBuilder.append("_");
				String stringDate = getFastaDate(record.getSequence().getCollectionDate());
				int year = (int) Double.parseDouble(stringDate);
				if (year < startYear) {
					startYear = year;
				}
				else if (year > endYear) {
					endYear = year;
				}
				tempBuilder.append(stringDate);
				tempBuilder.append("_");
				String normalizedLocation = Normalizer.normalizeLocation(record.getGeonameLocation());
				tempBuilder.append(normalizedLocation);
				if (isUsingDefaultGLM) {
					addOccurrence(normalizedLocation);
				}
				if (geonameCoordinates != null && geonameCoordinates.get(normalizedLocation) == null) {
					String coordinates = normalizedLocation+"\t"+record.getGeonameLocation().getLatitude()+"\t"+record.getGeonameLocation().getLongitude();
					geonameCoordinates.put(normalizedLocation, coordinates);
					if(isUsingDefaultGLM) {
						uniqueLocations.add(record.getGeonameLocation());
					}
					uniqueGeonames.add(normalizedLocation);
				}
				tempBuilder.append("\n");
				List<String> rows = breakUp(record.getSequence().getRawSequence());
				for (String row : rows) {
					tempBuilder.append(row);
					tempBuilder.append("\n");
				}
				tempBuilder.append("\n");
				builder.append(tempBuilder.toString());
			}
			catch (Exception e) {
				log.log(Level.SEVERE, "Error Fasta Formatting: "+e.getMessage());
				throw new AlignerException("Error running mafft: "+e.getMessage(), null);
			}
		}
		log.info("Fasta Formatting complete.");
		return builder.toString();
	}
	
	/**
	 * Combines the Custom records' sequences into a FASTA formatted String
	 * @param records List of full GenBankRecords
	 * @return String FASTA formatted sequences
	 * @throws AlignerException 
	 * @throws Exception 
	 */
	private String convertCustomRecordToFasta(List<FastaRecord> records, boolean isUsingDefaultGLM) throws AlignerException {
		log.info("Starting Fasta formatting");
		StringBuilder builder = new StringBuilder();
		StringBuilder tempBuilder;
		for (FastaRecord record : records) {
			try {
				tempBuilder = new StringBuilder();
				tempBuilder.append(">");
				tempBuilder.append(record.getAccession());
				tempBuilder.append("_");
				String humanDateFormat = SecurityHelper.FASTA_MET_DECIMAL_DATE_REGEX;
				Pattern regex = Pattern.compile(humanDateFormat);
				Matcher matcher = regex.matcher(record.getCollectionDate());
				String stringDate;
				if(matcher.matches()){
					stringDate = record.getCollectionDate();
				} else {
					stringDate = getFastaDate(record.getCollectionDate());
				}
				int year = (int) Double.parseDouble(stringDate);
				if (year < startYear) {
					startYear = year;
				}
				else if (year > endYear) {
					endYear = year;
				}
				tempBuilder.append(stringDate);
				tempBuilder.append("_");
				String normalizedLocation = Normalizer.normalizeLocation(record.getGeonameLocation());
				tempBuilder.append(normalizedLocation);
				if (isUsingDefaultGLM) {
					addOccurrence(normalizedLocation);
				}
				if (geonameCoordinates != null && geonameCoordinates.get(normalizedLocation) == null) {
					String coordinates = normalizedLocation+"\t"+record.getGeonameLocation().getLatitude()+"\t"+record.getGeonameLocation().getLongitude();
					geonameCoordinates.put(normalizedLocation, coordinates);
					if(isUsingDefaultGLM) {
						uniqueLocations.add(record.getGeonameLocation());
					}
					uniqueGeonames.add(normalizedLocation);
				}
				tempBuilder.append("\n");
				List<String> rows = breakUp(record.getRawSequence());
				for (String row : rows) {
					tempBuilder.append(row);
					tempBuilder.append("\n");
				}
				tempBuilder.append("\n");
				builder.append(tempBuilder.toString());
			}
			catch (Exception e) {
				log.log(Level.SEVERE, "Error Fasta Formatting: "+e.getMessage());
				throw new AlignerException("Error running mafft: "+e.getMessage(), null);
			}
		}
		log.info("Fasta Formatting complete.");
		return builder.toString();
	}
	
	/**
	 * Converts a raw String Date to FASTA formatted Decimal Date 
	 * @param collectionDate
	 * @return decimal date
	 * @throws AlignerException 
	 * @throws NormalizerException 
	 * @throws Exception 
	 */
	private String getFastaDate(String collectionDate) throws AlignerException, NormalizerException {
		if (collectionDate != null) {
			String date = Normalizer.formatDate(collectionDate);
			return Normalizer.dateToDecimal(date);
		}
		else {
			throw new AlignerException("Unkown Date!", null);
		}
		
	}

	/**
	 * Breaks up sequences into 80 character lines
	 * @param sequence raw rna/dna sequence
	 * @return sequence split into 80 character lines
	 * @throws AlignerException 
	 * @throws Exception 
	 */
	private List<String> breakUp(String sequence) throws AlignerException {
		// convert to lowercase?
		sequence = sequence.toLowerCase();
		LinkedList<String> segments = new LinkedList<String>();
		int length = sequence.length();
		int i;
		for (i = 0; i+80 < length; i+=80) {
			segments.add(sequence.substring(i, i+80));
		}
		segments.add(sequence.substring(i));
		int count = 0;
		for (String s : segments) {
			count+= s.length();
		}
		if (count != length) {
			throw new AlignerException("Error breaking up sequence. Result was "+count+" length instead of the expected "+length+" length.", null);
		}
		return segments;
	}
	
	/**
	 * Generate raw FASTA for downlaods
	 * @param accessions
	 * @return raw FASTA
	 * @throws GenBankRecordNotFoundException
	 * @throws DaoException
	 * @throws PipelineException
	 */
/*	public String generateDownloadableRawFasta(List<String> accessions) throws GenBankRecordNotFoundException, DaoException, PipelineException {
		List<GenBankRecord> records = loadSequences(accessions, false, false);
		return fastaFormat(records, false);
	}
	*/
	/**
	 * Adds an occurrence of a GLM state
	 * @param state
	 */
	private void addOccurrence(String state) {
		Integer currentCount = occurrences.get(state);
		if (currentCount == null) {
			occurrences.put(state, 1);
		}
		else {
			occurrences.put(state, currentCount+1);
		}
	}

}
