package edu.asu.zoophy.rest.security;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Repository;

import edu.asu.zoophy.rest.pipeline.XMLParameters;

/**
 * Helps with basic security features such as screening input
 * @author devdemetri
 */
@Repository("SecurityHelper")
public class SecurityHelper {
	
	public static final String ACCESSION_REGEX = "^([A-Z]|\\d|_|\\.){5,10}+$";
	public static final String LUCENE_REGEX = "^(\\w| |:|\\[|\\]|\\(|\\)){5,5000}+$";
	public static final String EMAIL_REGEX = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]++$";
	public static final String JOB_NAME_REGEX = "^(\\w| |-|_|#|&){3,255}+$";
	public static final String JOB_ID_REGEX = "^\\w{9}+-\\w{4}+-\\w{4}+-\\w{4}+-\\w{12}+$";
	public static final String FASTA_MET_RID_REGEX = "^(\\w|\\d){1,20}?$";
	public static final String FASTA_MET_DECIMAL_DATE_REGEX = "(\\d{4}(\\.\\d{1,10})?)";
	public static final String FASTA_MET_MMDDYYYY_DATE_REGEX = "(((0?[1-9]|1[012])\\-)?(((0?[1-9]|1[012])\\-)?(0?[1-9]|[12][0-9]|3[01])\\-)\\d{4})";
	public static final String FASTA_MET_HUMAN_DATE_REGEX = "(((0[1-9]|[12][0-9]|3[01])\\-)?((Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\-)?\\d{4})";
//			+ "|"+ "((\\d){4}-((0?[1-9]|1[012])\\-)(0[1-9]|[12][0-9]|3[01]))";
	public static final String FASTA_DATE_REGEX = "((0[1-9]|[12][0-9]|3[01])\\-((Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\-)\\d{4})" + 
												"|" + "(((Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\-)\\d{4})" +
												"|" + "\\d{4}";
	public static final String FASTA_MET_DATE_REGEX = "^(" + FASTA_MET_DECIMAL_DATE_REGEX + "|" + FASTA_MET_MMDDYYYY_DATE_REGEX + "|" + FASTA_MET_HUMAN_DATE_REGEX + ")$";
	public static final String FASTA_MET_GEOID_REGEX = "(\\d){4,10}";
	public static final String FASTA_MET_LOCNAME_REGEX = "([\\w-.,'’ ]){1,30}";
	public static final String FASTA_MET_LOC_REGEX = "^(" + FASTA_MET_GEOID_REGEX + "|" + FASTA_MET_LOCNAME_REGEX + ")$";
	public static final String FASTA_MET_RSEQ_REGEX = "^([ACGTYNKacgtynk-]){1,20000}$";
	
	/**
	 * Verifies parameters via regular expression
	 * @param parameter - String parameter to check
	 * @param type - Type of allowed parameter
	 * @return True if parameter passes regular expression validation
	 */
	public Boolean checkParameter(String parameter, Parameter type) {
		if (parameter == null || parameter.trim().isEmpty()) {
			return false;
		}
		Pattern regex;
		switch (type) {
			case ACCESSION:
				regex = Pattern.compile(ACCESSION_REGEX);
				break;
			case LUCENE_QUERY:
				regex = Pattern.compile(LUCENE_REGEX);
				break;
			case EMAIL:
				regex = Pattern.compile(EMAIL_REGEX);
				break;
			case JOB_NAME:
				regex = Pattern.compile(JOB_NAME_REGEX);
				break;
			case JOB_ID:
				regex = Pattern.compile(JOB_ID_REGEX);
				break;
			case RECORD_ID:
				regex = Pattern.compile(FASTA_MET_RID_REGEX);
				break;
			case LOCATION:
				regex = Pattern.compile(FASTA_MET_LOC_REGEX);
				break;
			case RAW_SEQUENCE:
				regex = Pattern.compile(FASTA_MET_RSEQ_REGEX);
				break;
			case DATE:
				regex = Pattern.compile(FASTA_MET_DATE_REGEX);
				break;
			default:
				return false;
		}
		Matcher matcher = regex.matcher(parameter);
		return matcher.matches();
	}

	/**
	 * Validates XML values
	 * @param xmlOptions XML Options to validate
	 * @throws ParameterException if any XML values are invalid
	 */
	public void verifyXMLOptions(XMLParameters xmlOptions) throws ParameterException {
		if (xmlOptions == null) {
			throw new ParameterException("Missing XML Parameters!");
		}
		if (!xmlOptions.isDefault()) {
			if (xmlOptions.getChainLength() == null) {
				throw new ParameterException("Missing XML Chain Length!");
			}
			else if (xmlOptions.getChainLength() < 10000000 || xmlOptions.getChainLength() > 250000000) {
				throw new ParameterException("Invalid XML Chain Length!");
			}
			if (xmlOptions.getSubSampleRate() == null) {
				throw new ParameterException("Missing XML Sub Sample Rate!");
			}
			else if (xmlOptions.getSubSampleRate() < 1000 || xmlOptions.getSubSampleRate() > 25000) {
				throw new ParameterException("Invalid XML Sub Sample Rate!");
			}
			if (xmlOptions.getSubstitutionModel() == null) {
				throw new ParameterException("Missing XML Substitution Model!");
			}
		}
	}

}
