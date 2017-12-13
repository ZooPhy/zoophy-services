package edu.asu.zoophy.rest.pipeline;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import edu.asu.zoophy.rest.custom.FastaRecord;
import edu.asu.zoophy.rest.pipeline.utils.Normalizer;
import edu.asu.zoophy.rest.pipeline.utils.NormalizerException;

/**
 * Responsible for aligning sequences into FASTA format
 * @author amagge
 */
public class CustomSequenceAligner {

	private final String JOB_LOG_DIR;
	private final ZooPhyJob job;
	private final Logger log;
	private File logFile;
	private Set<String> uniqueGeonames;
	private Map<String,String> geonameCoordinates;
	private int startYear = 3000;
	private int endYear = 1000;
	private Map<String, Integer> occurrences = null;
	
	/**
	 * Constructor for regular ZooPhy Pipeline usage
	 * @param job - ZooPhyJob for Predictor data
	 * @param dao - ZooPhyDAO for SQL operations
	 * @param indexSearcher - LuceneSearcher for index operations
	 * @throws PipelineException
	 */
	public CustomSequenceAligner(ZooPhyJob job) throws PipelineException {
		PropertyProvider provider = PropertyProvider.getInstance();
		JOB_LOG_DIR = provider.getProperty("job.logs.dir");
		log = Logger.getLogger("CustomSequenceAligner"+job.getID());
		uniqueGeonames = new LinkedHashSet<String>();
		geonameCoordinates = new HashMap<String,String>();
		if (job.isUsingGLM() && !job.isUsingCustomPredictors()) {
			occurrences = new HashMap<String, Integer>();
		}
		this.job = job;
	}
	
	/**
	 * Sequence Alignment Pipeline for non-genbank FASTA that runs:
	 * 1) Geoname Disjoiner
	 * 2) GLM Predictor Generator (only if job is using GLM)
	 * 3) FASTA formatting of raw sequences
	 * 4) MAFFT sequence alignment
	 * @param accessions - record sequences to be included in FASTA
	 * @param isTest - True iff actual alignment can be skipped for a test run
	 * @return Final List of Records to be used in the Job
	 * @throws PipelineException
	 */
	public List<FastaRecord> align(List<FastaRecord> recs, boolean isTest) throws PipelineException {
		FileHandler fileHandler = null;
		try {
			logFile = new File(JOB_LOG_DIR+job.getID()+".log");
			fileHandler = new FileHandler(JOB_LOG_DIR+job.getID()+".log", true);
			SimpleFormatter formatter = new SimpleFormatter();  
	        fileHandler.setFormatter(formatter);
	        log.addHandler(fileHandler);
	        log.setUseParentHandlers(false);
			log.info("Starting Mafft Job: "+job.getID());
//			List<GenBankRecord> recs = loadSequences(accessions, true, (job.isUsingGLM() && !job.isUsingCustomPredictors()));
			log.info("After screening job includes: "+recs.size()+" records.");
			String rawFasta = convertCustomRecordToFasta(recs, (job.isUsingGLM() && !job.isUsingCustomPredictors()));
			createCoordinatesFile();
			if (isTest) {
				fakeMafft(rawFasta);
			}
			else {
				runMafft(rawFasta);
			}
			log.info("Mafft Job: "+job.getID()+" has finished.");
			log.info("Deleting raw fasta...");
			try {
				Path path = Paths.get(System.getProperty("user.dir")+"/ZooPhyJobs/"+job.getID()+"-raw.fasta");
				Files.delete(path);
			}
			catch (IOException e) {
				log.log(Level.SEVERE, "ERROR! could not delete raw fasta: "+e.getMessage());
				throw e;
			}
			log.info("Mafft process complete");
			fileHandler.close();
			return recs;
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
	 * Creates the coordinates file needed for SpreaD3
	 */
	private void createCoordinatesFile() {
		StringBuilder coordinates = new StringBuilder();
		for (String location : uniqueGeonames) {
			coordinates.append(geonameCoordinates.get(location));
			coordinates.append("\n");
		}
		coordinates.trimToSize();
		String dir = System.getProperty("user.dir")+"/ZooPhyJobs/"+job.getID()+"-";
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
	 * Runs MAFFT to Align Sequences
	 * @param rawFasta - String of raw fasta formatted sequences
	 * @return file path to MAFFT aligned .fasta file
	 * @throws AlignerException 
	 */
	private String runMafft(String rawFasta) throws AlignerException {
		log.info("Setting up Mafft for job: "+job.getID());
		String dir = System.getProperty("user.dir")+"/ZooPhyJobs/"+job.getID()+"-";
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
	 * To save time, the raw fasta is copied to an aligned fasta file instead of actually running Mafft
	 * Useful for quickly validating job parameters before starting job
	 * @param rawFasta
	 * @return file path to fake aligned .fasta file that is really just a copy of the raw .fasta file
	 */
	private String fakeMafft(String rawFasta) {
		log.info("Faking Mafft for job: "+job.getID());
		String dir = System.getProperty("user.dir")+"/ZooPhyJobs/"+job.getID()+"-";
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
//				String stringDate = getFastaDate(record.getCollectionDate());
				String stringDate = record.getCollectionDate();
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
