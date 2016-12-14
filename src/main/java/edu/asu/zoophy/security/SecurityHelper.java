package edu.asu.zoophy.security;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Repository;

/**
 * Helps with basic security features such as screening input
 * @author devdemetri
 */
@Repository("SecurityHelper")
public class SecurityHelper {
	
	private static final String ACCESSION_REGEX = "^([A-Z]|\\d|_|\\.){5,10}+$";
	private static final String LUCENE_REGEX = "^(\\w| |:|\\[|\\]|\\(|\\)){5,5000}+$";
	private static final String EMAIL_REGEX = "^[-a-z0-9~!$%^&*_=+}{\\'?]+(\\.[-a-z0-9~!$%^&*_=+}{\\'?]+)*@([a-z0-9_][-a-z0-9_]*(\\.[-a-z0-9_]+[a-z][a-z])|([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}))(:[0-9]{1,5})?$";
	private static final String JOB_NAME_REGEX = "^(\\w| |-|_|#|&){3,255}+$";
	
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
			default:
				return false;
		}
		Matcher matcher = regex.matcher(parameter);
		return matcher.matches();
	}

}
