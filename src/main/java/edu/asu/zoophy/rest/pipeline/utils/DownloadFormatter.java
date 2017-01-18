package edu.asu.zoophy.rest.pipeline.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.asu.zoophy.rest.database.ZooPhyDAO;
import edu.asu.zoophy.rest.genbank.GenBankRecord;
import edu.asu.zoophy.rest.index.LuceneSearcher;
import edu.asu.zoophy.rest.index.LuceneSearcherException;
import edu.asu.zoophy.rest.pipeline.SequenceAligner;
import edu.asu.zoophy.rest.security.ParameterException;

@Component("DownloadFormatter")
public class DownloadFormatter {
	
	@Autowired
	private ZooPhyDAO dao;
	
	@Autowired
	private LuceneSearcher indexSearcher;
	
	private final static Logger log = Logger.getLogger("DownloadFormatter");

	/**
	 * Generate String for given download format
	 * @param accessions
	 * @param format
	 * @return Download String
	 * @throws ParameterException
	 * @throws FormatterException
	 */
	public String generateDownload(List<String> accessions, DownloadFormat format) throws ParameterException, FormatterException {
		String result = null;
		try {
			switch (format) {
				case CSV:
					result = generateCSV(accessions);
					break;
				case FASTA:
					result = generateFASTA(accessions);
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
	private String generateCSV(List<String> accessions) throws LuceneSearcherException, FormatterException {
		try {
			List<GenBankRecord> records = new LinkedList<GenBankRecord>();
			for (String accession : accessions) {
				GenBankRecord record = indexSearcher.getRecord(accession);
				if (record != null) {
					records.add(record);
				}
			}
			StringBuilder csv = new StringBuilder("Accession,Genes,Virus,Date,Host,Country,Segment Length\n");
			for (GenBankRecord record : records) {
				csv.append(Normalizer.csvify(record.getAccession()));
				csv.append(",");
				csv.append(Normalizer.csvify(Normalizer.geneListToCSVString(record.getGenes())));
				csv.append(",");
				csv.append(Normalizer.csvify(Normalizer.simplifyOrganism(record.getSequence().getOrganism())));
				csv.append(",");
				csv.append(Normalizer.csvify(Normalizer.normalizeDate(record.getSequence().getCollectionDate())));
				csv.append(",");
				if (record.getHost() != null && record.getHost().getName() != null) {
					csv.append(Normalizer.csvify(record.getHost().getName()));
				}
				else {
					csv.append(Normalizer.csvify("unknown"));
				}
				csv.append(",");
				if (record.getGeonameLocation() != null && record.getGeonameLocation().getCountry() != null) {
					csv.append(Normalizer.csvify(record.getGeonameLocation().getCountry()));
				}
				else {
					csv.append(Normalizer.csvify("unknown"));
				}
				csv.append(",");
				csv.append(Normalizer.csvify(String.valueOf(record.getSequence().getSegmentLength())));
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
	 * Generates a FASTA String for downloads
	 * @param accessions
	 * @return FASTA String for downloads
	 * @throws FormatterException
	 */
	private String generateFASTA(List<String> accessions) throws FormatterException {
		try {
			SequenceAligner fastaGenerator = new SequenceAligner(dao, indexSearcher);
			return fastaGenerator.generateDownloadableRawFasta(accessions);
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Error generating FASTA: "+e.getMessage());
			throw new FormatterException("Error Generating FASTA!");
		}
	}
	
}
