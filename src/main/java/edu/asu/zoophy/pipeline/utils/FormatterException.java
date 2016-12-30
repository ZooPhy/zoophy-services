package edu.asu.zoophy.pipeline.utils;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception for Download Formatter errors
 * @author devdemetri
 */
@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR)
public class FormatterException extends Exception {

	private static final long serialVersionUID = 5766527788348659912L;

	public FormatterException(String message) {
		super(message);
	}
	
}
