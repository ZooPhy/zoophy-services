package edu.asu.zoophy.rest.pipeline;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception for BEAST XML Parameter modification errors
 * @author devdemetri
 */
@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR)
public class XMLParameterException extends PipelineException {

	private static final long serialVersionUID = -2848276814035287427L;

	public XMLParameterException(String message, String userMessage) {
		super(message, userMessage);
	}

}
