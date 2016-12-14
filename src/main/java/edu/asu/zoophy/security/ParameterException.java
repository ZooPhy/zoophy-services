package edu.asu.zoophy.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception for bad input parameters
 * @author devdemetri
 */
@ResponseStatus(value=HttpStatus.BAD_REQUEST)
public class ParameterException extends Exception {

	private static final long serialVersionUID = -7398206364233388890L;
	
	public ParameterException(String parameter) {
		super("Bad Parameter: "+parameter);
	}

}
