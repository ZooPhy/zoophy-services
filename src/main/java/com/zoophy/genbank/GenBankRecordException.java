package com.zoophy.genbank;

/**
 * Custom Exception for GenBankRecord problems
 * @author devdemetri
 */
public class GenBankRecordException extends Exception {

	private static final long serialVersionUID = 3490823521446043161L;
	
	public GenBankRecordException(String message) {
		super(message);
	}

}
