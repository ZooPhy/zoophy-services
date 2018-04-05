package edu.asu.zoophy.rest.pipeline.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.asu.zoophy.rest.database.ZooPhyDAO;
import edu.asu.zoophy.rest.genbank.GenBankRecord;
import edu.asu.zoophy.rest.index.LuceneHierarchySearcher;
import edu.asu.zoophy.rest.index.LuceneSearcher;
import edu.asu.zoophy.rest.index.LuceneSearcherException;
import edu.asu.zoophy.rest.pipeline.AlignerException;
import edu.asu.zoophy.rest.pipeline.SequenceAligner;
import edu.asu.zoophy.rest.security.ParameterException;

@Component("DownloadFormatter")
public class DownloadFormatter {
	
	@Autowired
	private ZooPhyDAO dao;
	
	@Autowired
	private LuceneSearcher indexSearcher;
	
	@Autowired
	private LuceneHierarchySearcher hierarchyIndexSearcher;
	
	private final static Logger log = Logger.getLogger("DownloadFormatter");
	
	private int startYear = 3000;
	private int endYear = 1000;

	/**
	 * Generate String for given download format
	 * @param accessions
	 * @param format
	 * @return Download String
	 * @throws ParameterException
	 * @throws FormatterException
	 */
	public String generateDownload(List<String> accessions, List<String> columns, DownloadFormat format) throws ParameterException, FormatterException {
		String result = null;
		try {
			switch (format) {
				case CSV:
					result = generateCSV(accessions, columns);
					break;
				case FASTA:
					result = generateFASTA(accessions, columns);
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
	 * @return CSV String for downloads
	 * @throws LuceneSearcherException
	 * @throws FormatterException 
	 */
	private String generateCSV(List<String> accessions, List<String> columns) throws LuceneSearcherException, FormatterException {
		try {
			List<GenBankRecord> records = new LinkedList<GenBankRecord>();
			for (String accession : accessions) {
				GenBankRecord record = indexSearcher.getRecord(accession);
				if (record != null) {
					records.add(record);
				}
			}
			
			StringJoiner stringJoiner = new StringJoiner(",");
			StringBuilder csv = new StringBuilder();
			for(String column: columns) {
				stringJoiner.add(column);
			}
			csv.append(stringJoiner);
			csv.append("\n");
			
			for (GenBankRecord record : records) {
				stringJoiner = new StringJoiner(",");
				if(columns.contains(DownloadColumn.ID)) {
					stringJoiner.add(Normalizer.csvify(record.getAccession()));
				}
				if(columns.contains(DownloadColumn.GENES)) {
					stringJoiner.add(Normalizer.csvify(Normalizer.geneListToCSVString(record.getGenes())));
				}
				if(columns.contains(DownloadColumn.VIRUS_ID)) {
					stringJoiner.add(Normalizer.csvify(record.getSequence().getTaxID().toString()));
				}
				if(columns.contains(DownloadColumn.VIRUS)) {
					stringJoiner.add(Normalizer.csvify(Normalizer.simplifyOrganism(record.getSequence().getOrganism())));
				}
				if(columns.contains(DownloadColumn.DATE)) {
					stringJoiner.add(Normalizer.csvify(Normalizer.normalizeDate(record.getSequence().getCollectionDate())));
				}
				if(columns.contains(DownloadColumn.HOST_ID)) {
					stringJoiner.add(Normalizer.csvify(record.getHost().getTaxon().toString()));
				}
				if(columns.contains(DownloadColumn.HOST)) {
					if (record.getHost() != null && record.getHost().getName() != null) {
						stringJoiner.add(Normalizer.csvify(record.getHost().getName()));
					}
					else {
						stringJoiner.add(Normalizer.csvify("unknown"));
					}
				}
				if(columns.contains(DownloadColumn.COUNTRY)) {
					if (record.getGeonameLocation() != null && record.getGeonameLocation().getCountry() != null) {
						stringJoiner.add(Normalizer.csvify(record.getGeonameLocation().getCountry()));
					}
					else {
						stringJoiner.add(Normalizer.csvify("unknown"));
					}
				}
				if(columns.contains(DownloadColumn.LENGTH)) {
					stringJoiner.add(Normalizer.csvify(String.valueOf(record.getSequence().getSegmentLength())));
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
	 * @param records List of full GenBankRecords
	 * @return String FASTA formatted sequences
	 * @throws AlignerException 
	 * @throws FormatterException 
	 * @throws Exception 
	 */
	private String generateFASTA(List<String> accessions, List<String> columns) throws AlignerException, FormatterException {
		
		try {
			SequenceAligner fastaGenerator = new SequenceAligner(dao, hierarchyIndexSearcher);
			List<GenBankRecord> records = fastaGenerator.loadSequences(accessions, null, false, false);
		
			log.info("Starting Fasta formatting");
			StringBuilder builder = new StringBuilder();
			StringBuilder tempBuilder;
			StringJoiner stringJoiner;
			
			for (GenBankRecord record : records) {
				stringJoiner = new StringJoiner("|");
				tempBuilder = new StringBuilder();
				
				try {
					tempBuilder.append(">");
					if(columns.contains(DownloadColumn.ID)) {
						stringJoiner.add(record.getAccession());
					}
					if(columns.contains(DownloadColumn.VIRUS_ID)) {
						stringJoiner.add(record.getSequence().getTaxID().toString());	
					}
					if(columns.contains(DownloadColumn.VIRUS)) {
						stringJoiner.add(Normalizer.simplifyOrganism(record.getSequence().getOrganism()));
					}
					if(columns.contains(DownloadColumn.HOST_ID)) {
						stringJoiner.add(record.getHost().getTaxon().toString());	
					}
					if(columns.contains(DownloadColumn.HOST)) {
						if (record.getHost() != null && record.getHost().getName() != null) {	
							stringJoiner.add(record.getHost().getName());
						}
						else {
							stringJoiner.add("unknown");
					}						
					if(columns.contains(DownloadColumn.GENES)) {
						stringJoiner.add(Normalizer.geneListToCSVString(record.getGenes()));
					}
					if(columns.contains(DownloadColumn.LENGTH)) {
						stringJoiner.add(record.getSequence().getSegmentLength().toString());	
					}
					if(columns.contains(DownloadColumn.DATE)) {
						String stringDate = getFastaDate(record.getSequence().getCollectionDate());
						int year = (int) Double.parseDouble(stringDate);
						if (year < startYear) {
							startYear = year;
						}
						else if (year > endYear) {
							endYear = year;
						}
						stringJoiner.add(stringDate);
						}
					}
					if(columns.contains(DownloadColumn.COUNTRY)) {
						if (record.getGeonameLocation() != null && record.getGeonameLocation().getCountry() != null) {	
							String normalizedLocation = Normalizer.normalizeLocation(record.getGeonameLocation());		
							stringJoiner.add(normalizedLocation);
						}
						else {
							stringJoiner.add("unknown");
						}
					}
					tempBuilder.append(stringJoiner);
					tempBuilder.append("\n");
					if(columns.contains(DownloadColumn.RAW_SEQUENCE)) {
						List<String> rows = breakUp(record.getSequence().getRawSequence());
						for (String row : rows) {
							tempBuilder.append(row);
							tempBuilder.append("\n");
						}
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
		catch (Exception e) {
			log.log(Level.SEVERE, "Error generating CSV: "+e.getMessage());
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
	
}
