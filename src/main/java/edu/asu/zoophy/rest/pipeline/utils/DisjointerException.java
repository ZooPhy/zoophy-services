package edu.asu.zoophy.rest.pipeline.utils;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import edu.asu.zoophy.rest.pipeline.PipelineException;

/**
 * Custom exception for Geoname Disjointer errors
 * @author devdemetri
 */
@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR)
public class DisjointerException extends PipelineException {

	private static final long serialVersionUID = -6481210949889573944L;

	public DisjointerException(String message, String userMessage) {
		super(message, userMessage);
	}
	
}
