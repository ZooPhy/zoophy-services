package edu.asu.zoophy.rest.pipeline.utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.asu.zoophy.rest.JobConstants;
import edu.asu.zoophy.rest.JobRecord;
import edu.asu.zoophy.rest.database.DaoException;
import edu.asu.zoophy.rest.database.GenBankRecordNotFoundException;
import edu.asu.zoophy.rest.database.ZooPhyDAO;
import edu.asu.zoophy.rest.genbank.GenBankRecord;
import edu.asu.zoophy.rest.genbank.Location;
import edu.asu.zoophy.rest.genbank.Sequence;
import edu.asu.zoophy.rest.index.LuceneHierarchySearcher;
import edu.asu.zoophy.rest.index.LuceneSearcherException;
import edu.asu.zoophy.rest.pipeline.AlignerException;
import edu.asu.zoophy.rest.security.ParameterException;


/**
 * Responsible for formating records based on DownloadFormat and columns selected
 * @author amagge, kbhangal
 */
@Component("DownloadFormatter")
public class DownloadFormatter {
	
	@Autowired
	private ZooPhyDAO dao;
	
	@Autowired
	private LuceneHierarchySearcher hierarchyIndexSearcher;
	
	private final static Logger log = Logger.getLogger("DownloadFormatter");
	
	/**
	 * Generate String for given download format
	 * @param accessions
	 * @param columns
	 * @param format
	 * @return Download String
	 * @throws ParameterException
	 * @throws FormatterException
	 */
	public String generateDownload(List<JobRecord> records, List<String> columns, DownloadFormat format) throws ParameterException, FormatterException {
		String result = null;
		List<String > accessions = new ArrayList<>();
		List<GenBankRecord> fastaRecords = new ArrayList<>();
		
		for(JobRecord record : records) {
			if(record.getResourceSource()==JobConstants.SOURCE_GENBANK)
				accessions.add(record.getId());
			else {
				GenBankRecord genBankRecord = new GenBankRecord();
				Sequence sequence = new Sequence();
				sequence.setRawSequence(record.getRawSequence());
				sequence.setCollectionDate(record.getCollectionDate());
				Location location = new Location();
				location.setLocation(record.getGeonameID());
				
				genBankRecord.setAccession(record.getId());
				genBankRecord.setSequence(sequence);
				genBankRecord.setGeonameLocation(location);

				fastaRecords.add(genBankRecord);
			}
		}
		
		columns.add(0,DownloadColumn.ID);
		if(columns.size()==0) {
			columns.add(DownloadColumn.GENES);
			columns.add(DownloadColumn.VIRUS_ID);
			columns.add(DownloadColumn.VIRUS);
			columns.add(DownloadColumn.DATE);
			columns.add(DownloadColumn.HOST_ID);
			columns.add(DownloadColumn.HOST);
			columns.add(DownloadColumn.COUNTRY);
			columns.add(DownloadColumn.LENGTH);
		}
		try {
			switch (format) {
				case CSV:
					result = generateCSV(accessions, fastaRecords, columns);
					break;
				case FASTA:
					result = generateFASTA(accessions, fastaRecords, columns);
					break;
				default:
					log.log(Level.SEVERE, "Unimplemented format type: "+format.toString());
					throw new ParameterException(format.toString());
			}
			return result;
		}
		catch (ParameterException pe) {
			throw pe;
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Error generating download: "+e.getMessage());
			throw new FormatterException("Error Generating Download!");
		}
	}
	
	/**
	 * Generates a CSV String for downloads
	 * @param accessions
	 * @param columns
	 * @return CSV String for downloads
	 * @throws LuceneSearcherException
	 * @throws FormatterException 
	 */
	private String generateCSV(List<String> accessions, List<GenBankRecord> fastaRecords, List<String> columns) throws LuceneSearcherException, FormatterException {
		try {
			List<GenBankRecord> records = loadRecords(accessions);
			records.addAll(fastaRecords);
			
			//Headers
			StringJoiner stringJoiner = new StringJoiner(",");
			StringBuilder csv = new StringBuilder();
			for(String column: columns) {
				stringJoiner.add(column);
			}
			csv.append(stringJoiner);
			csv.append("\n");
			
			for (GenBankRecord record : records) {
				Location location = null;
				stringJoiner = new StringJoiner(",");
				for(String column: columns) {
					if((column.equals(DownloadColumn.COUNTRY) || column.equals(DownloadColumn.STATE)
							|| column.equals(DownloadColumn.GEONAMEID) || column.equals(DownloadColumn.LOCATION_HIERARCHY))
							&& location == null) {
						location = hierarchyIndexSearcher.findGeonameLocation(record.getGeonameLocation().getLocation());
					}
					stringJoiner.add(columnValue(record, column, location, DownloadFormat.CSV));	
				}
				csv.append(stringJoiner);
				csv.append("\n");
			}
			return csv.toString();
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Error generating CSV: "+e.getMessage());
			throw new FormatterException("Error Generating CSV!");
		}
	}
	
	/**
	 * Combines the records' sequences into a FASTA formatted String
	 * @param accessions
	 * @param columns
	 * @return String FASTA formatted sequences
	 * @throws AlignerException 
	 * @throws FormatterException 
	 * @throws Exception 
	 */
	private String generateFASTA(List<String> accessions, List<GenBankRecord> fastaRecords, List<String> columns) throws AlignerException, FormatterException {
		try {
			List<GenBankRecord> records = loadRecords(accessions);
			records.addAll(fastaRecords);
			
			columns.remove(DownloadColumn.RAW_SEQUENCE);
		
			log.info("Starting Fasta formatting");
			StringBuilder builder = new StringBuilder();
			StringBuilder tempBuilder;
			StringJoiner stringJoiner;
	
			for (GenBankRecord record : records) {
				Location location = null;
				stringJoiner = new StringJoiner("|");
				tempBuilder = new StringBuilder();
				tempBuilder.append(">");
				for(String column: columns) {
					if((column.equals(DownloadColumn.COUNTRY) || column.equals(DownloadColumn.STATE)
							|| column.equals(DownloadColumn.GEONAMEID) || 
							column.equals(DownloadColumn.LOCATION_HIERARCHY)) && location == null) {
						location = hierarchyIndexSearcher.findGeonameLocation(record.getGeonameLocation().getLocation());
					}
					stringJoiner.add(columnValue(record, column, location, DownloadFormat.FASTA));	
				}
				
				tempBuilder.append(stringJoiner);
				tempBuilder.append("\n");
				if(record.getSequence()!=null && record.getSequence().getRawSequence()!=null) {
					List<String> rows = breakUp(record.getSequence().getRawSequence());
					for (String row : rows) {
						tempBuilder.append(row);
						tempBuilder.append("\n");
					}
					tempBuilder.append("\n");
					builder.append(tempBuilder); 
				}else {
					builder.append("Unknown"); 
				}
			}
			log.info("Fasta Formatting complete.");
			return builder.toString();
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Error generating CSV: "+e.getMessage());
			throw new FormatterException("Error Generating CSV!");
		}
	}
	
	private List<GenBankRecord> loadRecords(List<String> accessions) throws GenBankRecordNotFoundException, DaoException{
		List<GenBankRecord> records = new LinkedList<GenBankRecord>();
		for (String accession : accessions) {
			GenBankRecord record = dao.retrieveFullRecord(accession);
			records.add(record);
		}
		return records;
	}
	
	/**
	 * Gets the value corresponding to the column from the record
	 * @param record
	 * @param column
	 * @param format
	 * @return String column value
	 * @throws AlignerException 
	 * @throws FormatterException 
	 * @throws NormalizerException 
	 */
	private String columnValue(GenBankRecord record, String column, Location location, DownloadFormat format) throws NormalizerException, FormatterException, AlignerException {
		switch(column) {
		case DownloadColumn.ID:
			if(record.getAccession()!=null)
				return  Normalizer.csvify(record.getAccession());
			else
				return "Unknown";
		case DownloadColumn.GENES:
			if(record.getGenes()!=null)
				return Normalizer.csvify(Normalizer.geneListToCSVString(record.getGenes()));
			else
				return "Unknown";
		case DownloadColumn.VIRUS_ID:
			if(record.getSequence()!=null && record.getSequence().getTaxID()!=null)
				return Normalizer.csvify(record.getSequence().getTaxID().toString());
			else
				return "Unknown";
		case DownloadColumn.VIRUS:
			if(record.getSequence()!=null && record.getSequence().getOrganism()!=null)
				return Normalizer.csvify(Normalizer.simplifyOrganism(record.getSequence().getOrganism()));
			else
				return "Unknown";
		case DownloadColumn.DATE:
			if(record.getSequence()!=null && record.getSequence().getCollectionDate()!=null) {
				if(format.equals(DownloadFormat.CSV)) {
					return Normalizer.csvify(Normalizer.formatDate(Normalizer.normalizeDate(record.getSequence().getCollectionDate())));
				}else if(format.equals(DownloadFormat.FASTA)) {
					return getFastaDate(record.getSequence().getCollectionDate());
				}
			}else {
				return "Unknown";
			}
		case DownloadColumn.HOST_ID:
			if(record.getHost()!=null && record.getHost().getTaxon()!=null) {
				return Normalizer.csvify(record.getHost().getTaxon().toString());
			}else {
				return "Unknown";
			}
		case DownloadColumn.HOST:
			if (record.getHost() != null && record.getHost().getName() != null) {
				return Normalizer.csvify(record.getHost().getName());
			}
			else {
				return Normalizer.csvify("unknown");
			}
		case DownloadColumn.GEONAMEID:
			if(location!=null) {
				return Normalizer.csvify(location.getGeonameID().toString());
			}else {
				return "Unknown";
			}
		case DownloadColumn.COUNTRY:
			if(location!=null) {
				return Normalizer.csvify(location.getCountry());
			}else {
				return "Unknown";
			}
		case DownloadColumn.STATE:
			if(location!=null) {
				return Normalizer.csvify(location.getState());
			}else {
				return "Unknown";
			}
		case DownloadColumn.LOCATION_HIERARCHY:
			if(location!=null) {
				return Normalizer.csvify(location.getHierarchy());
			}else {
				return "Unknown";
			}
		case DownloadColumn.LENGTH:
			if(record.getSequence()!=null && record.getSequence().getSegmentLength()!=null) {
				return Normalizer.csvify(String.valueOf(record.getSequence().getSegmentLength()));
			}else {
				return "Unknown";
			}
		default: 
			throw new FormatterException("Error Generating CSV!");
		}
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
		if (collectionDate != null && !collectionDate.equals("10000101")) {
			String date = Normalizer.formatDate(collectionDate);
			return Normalizer.dateToDecimal(date);
		}
		else {
			return "Unkown";
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
	
}
