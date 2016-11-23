package com.zoophy.database;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

/**
 * Custom exception for invalid GenBank record queries
 * @author devdemetri
 */
@ResponseStatus(value=HttpStatus.NOT_FOUND)
public class GenBankRecordNotFoundException extends Exception {

	private static final long serialVersionUID = 8236120491192852123L;

	public GenBankRecordNotFoundException(String accession) {
		super("GenBank record not found: "+accession);
	}
	
}
