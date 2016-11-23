package com.zoophy.security;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Repository;

/**
 * Helps with basic security features such as screening input
 * @author devdemetri
 */
@Repository("SecurityHelper")
public class SecurityHelper {
	
	private static final String ACCESSION_REGEX = "^([A-Z]|\\d|_|\\.){5,10}$";
	private static final String LUCENE_REGEX = "^(\\w| |:|\\[|\\]|\\(|\\)){5,1024}$";
	
	/**
	 * Verifies parameters via regular expression
	 * @param param - String parameter to check
	 * @param type - Type of allowed parameter
	 * @return - True if param passes regular expression validation
	 */
	public Boolean checkParameter(String param, Parameter type) {
		if (param == null || param.trim().isEmpty()) {
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
			default:
				return false;
		}
		Matcher match = regex.matcher(param);
		return match.matches();
	}

}
