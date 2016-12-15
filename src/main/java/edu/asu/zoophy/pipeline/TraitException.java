package edu.asu.zoophy.pipeline;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception for Discrete Trait Inserter errors
 * @author devdemetri
 */
@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR)
public class TraitException extends PipelineException {

	private static final long serialVersionUID = 7912093433369338407L;

	public TraitException(String message, String userMessage) {
		super(message, userMessage);
	}

}
