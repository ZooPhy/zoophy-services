package edu.asu.zoophy.rest.security;

/**
 * Types of allowed String parameters for services
 * @author devdemetri
 */
public enum Parameter {
	ACCESSION,
	LUCENE_QUERY, 
	EMAIL,
	JOB_NAME, 
	JOB_ID,
	RECORD_ID,
	LOCATION,
	RAW_SEQUENCE,
	DATE
}
