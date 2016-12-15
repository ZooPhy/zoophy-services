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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import edu.asu.zoophy.database.DaoException;
import edu.asu.zoophy.database.GenBankRecordNotFoundException;
import edu.asu.zoophy.database.ZoophyDAO;
import edu.asu.zoophy.genbank.GenBankRecord;
import edu.asu.zoophy.pipeline.utils.DisjointerException;
import edu.asu.zoophy.pipeline.utils.GeonameDisjointer;
import edu.asu.zoophy.pipeline.utils.Normalizer;
import edu.asu.zoophy.pipeline.utils.NormalizerException;

/**
 * Responsible for aligning sequences into FASTA format
 * @author devdemetri
 */
public class SequenceAligner {

	@Autowired
	private ZoophyDAO dao;
	
	private Logger log;
	private File logFile;
	
	@Value("${job.logs.dir}")
	private static String jobLogDir;
	
	private List<String> uniqueGeonames;
	private Map<String,String> geonameCoordinates;
	private final String jobID;
	
	public SequenceAligner(ZooPhyJob job) {
		jobID = job.getID();
		log = Logger.getLogger("MafftAligner");
		uniqueGeonames = new LinkedList<String>();
		geonameCoordinates = new HashMap<String,String>();
	}
	
	public String align(List<String> accessions) throws AlignerException {
		String alignedFasta;
		FileHandler fh = null;
		try {
			logFile = new File(jobLogDir+jobID+".log");
			fh = new FileHandler(jobLogDir+jobID+".log", true);
			SimpleFormatter formatter = new SimpleFormatter();  
	        fh.setFormatter(formatter);
	        log.addHandler(fh);
	        log.setUseParentHandlers(false);
			log.info("Starting Mafft Job: "+jobID);
			List<GenBankRecord>recs = loadSequences(accessions, true);
			String rawFasta = fastaFormat(recs);
			createCoordinatesFile();
			alignedFasta = runMafft(rawFasta);
			log.info("Mafft Job: "+jobID+" has finished.");
			log.info("Deleting raw fasta...");
			try {
				Path path = Paths.get(System.getProperty("user.dir")+"/ZooPhyJobs/"+jobID+"-raw.fasta");
				Files.delete(path);
			}
			catch (IOException e) {
				log.log(Level.SEVERE, "ERROR! could not delete raw fasta: "+e.getMessage());
				throw e;
			}
			log.info("Mafft process complete");
			fh.close();
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "ERROR! Mafft process failed: "+e.getMessage());
			throw new AlignerException(e.getMessage(), null);
		}
		finally {
			if (fh != null) {
				fh.close();
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
		String dir = System.getProperty("user.dir")+"/ZooPhyJobs/"+jobID+"-";
		try {
			PrintWriter out = new PrintWriter(dir+"coords.txt");
			out.write(coordinates.toString());
			out.close();
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Error setting up coords.txt: "+e.getMessage());
		}
	}

	protected List<GenBankRecord> loadSequences(List<String> accessions, boolean isDisjoint) throws GenBankRecordNotFoundException, DaoException, DisjointerException {
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
		GeonameDisjointer disjointer  = new GeonameDisjointer();
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
		log.info("Setting up Mafft for job: "+jobID);
		String dir = System.getProperty("user.dir")+"/ZooPhyJobs/"+jobID+"-";
		try {
			PrintWriter out = new PrintWriter(dir+"raw.fasta");
			out.write(rawFasta);
			out.close();
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Error setting up raw.fasta: "+e.getMessage());
		}
		String rawFile = dir + "raw.fasta";
		String outPath = dir+"aligned.fasta";
		File outFile = new File(outPath);
		try {
			ProcessBuilder pb = new ProcessBuilder("mafft", "--auto", rawFile);
			pb.redirectOutput(Redirect.appendTo(outFile));
			pb.redirectError(Redirect.appendTo(logFile));
			log.info("Running Mafft...");
			Process pr = pb.start();
			pr.waitFor();
			if (pr.exitValue() != 0) {
				log.log(Level.SEVERE, "Mafft failed! with code: "+pr.exitValue());
				throw new Exception("Mafft failed! with code: "+pr.exitValue());
			}
			log.info("Mafft finished.");
		} 
		catch (Exception e) {
			log.log(Level.SEVERE, "Error running mafft: "+e.getMessage());
			throw new AlignerException("Error running mafft: "+e.getMessage(), null);
		}
		return outPath;
	}

	/**
	 * @param records List of full GenBankRecords
	 * @return String Fasta formatted sequences
	 * @throws AlignerException 
	 * @throws Exception 
	 */
	protected String fastaFormat(List<GenBankRecord> records) throws AlignerException {
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
	 * 
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
		int len = sequence.length();
		int i;
		for (i = 0; i+80 < len; i+=80) {
			segments.add(sequence.substring(i, i+80));
		}
		segments.add(sequence.substring(i));
		int count = 0;
		for (String s : segments) {
			count+= s.length();
		}
		if (count != len) {
			log.log(Level.SEVERE, "Error breaking up sequence. Did not break correctly.");
			throw new AlignerException("Error breaking up sequence. Result was "+count+" length instead of the expected "+len+" length.", null);
		}
		return segments;
	}

}
