package edu.asu.zoophy.pipeline;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import edu.asu.zoophy.database.DaoException;
import edu.asu.zoophy.database.GenBankRecordNotFoundException;
import edu.asu.zoophy.database.ZoophyDAO;
import edu.asu.zoophy.genbank.GenBankRecord;
import edu.asu.zoophy.index.LuceneSearcher;
import edu.asu.zoophy.pipeline.utils.GeonameDisjointer;
import edu.asu.zoophy.pipeline.utils.Normalizer;
import edu.asu.zoophy.pipeline.utils.NormalizerException;

/**
 * Responsible for aligning sequences into FASTA format
 * @author devdemetri
 */
public class SequenceAligner {

	private final String JOB_LOG_DIR;
	private final String JOB_ID;
	private final ZoophyDAO dao;
	private final LuceneSearcher indexSearcher;
	private final Logger log;
	private File logFile;
	private List<String> uniqueGeonames;
	private Map<String,String> geonameCoordinates;
	
	public SequenceAligner(ZooPhyJob job, ZoophyDAO dao, LuceneSearcher indexSearcher) throws PipelineException {
		this.dao = dao;
		this.indexSearcher = indexSearcher;
		JOB_ID = job.getID();
		PropertyProvider provider = PropertyProvider.getInstance();
		JOB_LOG_DIR = provider.getProperty("job.logs.dir");
		log = Logger.getLogger("SequenceAligner");
		uniqueGeonames = new LinkedList<String>();
		geonameCoordinates = new HashMap<String,String>();
	}
	
	/**
	 * NOTE: Only use this constructor for generating downloadable FASTA, not for ZooPhy Jobs
	 * @param dao
	 * @param indexSearcher
	 */
	public SequenceAligner(ZoophyDAO dao, LuceneSearcher indexSearcher) {
		log = Logger.getLogger("SequenceAligner");
		this.dao = dao;
		this.indexSearcher = indexSearcher;
		JOB_ID = null;
		JOB_LOG_DIR = null;
	}
	
	/**
	 * 
	 * @param accessions
	 * @return
	 * @throws AlignerException
	 */
	public String align(List<String> accessions) throws AlignerException {
		String alignedFasta;
		FileHandler fileHandler = null;
		try {
			logFile = new File(JOB_LOG_DIR+JOB_ID+".log");
			fileHandler = new FileHandler(JOB_LOG_DIR+JOB_ID+".log", true);
			SimpleFormatter formatter = new SimpleFormatter();  
	        fileHandler.setFormatter(formatter);
	        log.addHandler(fileHandler);
	        log.setUseParentHandlers(false);
			log.info("Starting Mafft Job: "+JOB_ID);
			List<GenBankRecord>recs = loadSequences(accessions, true);
			String rawFasta = fastaFormat(recs);
			createCoordinatesFile();
			alignedFasta = runMafft(rawFasta);
			log.info("Mafft Job: "+JOB_ID+" has finished.");
			log.info("Deleting raw fasta...");
			try {
				Path path = Paths.get(System.getProperty("user.dir")+"/ZooPhyJobs/"+JOB_ID+"-raw.fasta");
				Files.delete(path);
			}
			catch (IOException e) {
				log.log(Level.SEVERE, "ERROR! could not delete raw fasta: "+e.getMessage());
				throw e;
			}
			log.info("Mafft process complete");
			fileHandler.close();
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "ERROR! Mafft process failed: "+e.getMessage());
			throw new AlignerException(e.getMessage(), null);
		}
		finally {
			if (fileHandler != null) {
				fileHandler.close();
			}
		}
		return alignedFasta;
	}
	
	private void createCoordinatesFile() {
		StringBuilder coordinates = new StringBuilder();
		for (String location : uniqueGeonames) {
			coordinates.append(geonameCoordinates.get(location));
			coordinates.append("\n");
		}
		coordinates.trimToSize();
		String dir = System.getProperty("user.dir")+"/ZooPhyJobs/"+JOB_ID+"-";
		try {
			PrintWriter out = new PrintWriter(dir+"coords.txt");
			out.write(coordinates.toString());
			out.close();
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Error setting up coords.txt: "+e.getMessage());
		}
	}

	/**
	 * 
	 * @param accessions
	 * @param isDisjoint
	 * @return
	 * @throws GenBankRecordNotFoundException
	 * @throws DaoException
	 * @throws PipelineException
	 */
	private List<GenBankRecord> loadSequences(List<String> accessions, boolean isDisjoint) throws GenBankRecordNotFoundException, DaoException, PipelineException {
		log.info("Loading records for Mafft...");
		List<GenBankRecord> records = new LinkedList<GenBankRecord>();
		for (String accession : accessions) {
			GenBankRecord record = dao.retrieveFullRecord(accession);
			try {
				if (record != null && record.getSequence().getCollectionDate() != null && !getFastaDate(record.getSequence().getCollectionDate()).equalsIgnoreCase("unknown") && record.getGeonameLocation() != null) { 
					records.add(record);
				}
			}
			catch (Exception e) {
				log.log(Level.SEVERE, "ERROR! Issue Adding Record: "+accession+" : "+e.getMessage());
			}
		}
		log.info("Records loaded.");
		if (isDisjoint) {
		GeonameDisjointer disjointer  = new GeonameDisjointer(indexSearcher);
			return disjointer.disjointRecords(records);
		}
		else {
			for (int i = 0; i < records.size(); i++) {
				GenBankRecord record = records.get(i);
				if (record.getGeonameLocation() == null || Normalizer.normalizeLocation(record.getGeonameLocation()).equalsIgnoreCase("unknown")) {
					records.remove(i);
				}
			}
			return records;
		}
	}

	/**
	 * @param rawFasta String of raw fasta formatted sequences
	 * @return String of MAFFT aligned fasta sequences
	 * @throws Exception 
	 */
	private String runMafft(String rawFasta) throws AlignerException {
		log.info("Setting up Mafft for job: "+JOB_ID);
		String dir = System.getProperty("user.dir")+"/ZooPhyJobs/"+JOB_ID+"-";
		try {
			PrintWriter printer = new PrintWriter(dir+"raw.fasta");
			printer.write(rawFasta);
			printer.close();
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Error setting up raw.fasta: "+e.getMessage());
		}
		String rawFilePath = dir + "raw.fasta";
		String alignedFilePath = dir+"aligned.fasta";
		File outFile = new File(alignedFilePath);
		try {
			ProcessBuilder builder = new ProcessBuilder("mafft", "--auto", rawFilePath);
			builder.redirectOutput(Redirect.appendTo(outFile));
			builder.redirectError(Redirect.appendTo(logFile));
			log.info("Running Mafft...");
			Process mafftProcess = builder.start();
			PipelineManager.setProcess(JOB_ID, mafftProcess);
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
	 * @param records List of full GenBankRecords
	 * @return String Fasta formatted sequences
	 * @throws AlignerException 
	 * @throws Exception 
	 */
	private String fastaFormat(List<GenBankRecord> records) throws AlignerException {
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
				tempBuilder.append(getFastaDate(record.getSequence().getCollectionDate()));
				tempBuilder.append("_");
				String normalizedLocation = Normalizer.normalizeLocation(record.getGeonameLocation());
				tempBuilder.append(normalizedLocation);
				if (geonameCoordinates != null && geonameCoordinates.get(normalizedLocation) == null) {
					String coordinates = normalizedLocation+"\t"+record.getGeonameLocation().getLatitude()+"\t"+record.getGeonameLocation().getLongitude();
					geonameCoordinates.put(normalizedLocation, coordinates);
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
	 * Generate raw FASTA for downlaods
	 * @param accessions
	 * @return raw FASTA
	 * @throws GenBankRecordNotFoundException
	 * @throws DaoException
	 * @throws PipelineException
	 */
	public String generateDownloadableRawFasta(List<String> accessions) throws GenBankRecordNotFoundException, DaoException, PipelineException {
		List<GenBankRecord> records = loadSequences(accessions, false);
		return fastaFormat(records);
	}

}
