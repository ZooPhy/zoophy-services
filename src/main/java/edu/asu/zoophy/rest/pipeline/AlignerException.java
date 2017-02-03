package edu.asu.zoophy.rest.pipeline;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception for Sequence Aligner errors
 * @author devdemetri
 */
@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR)
public class AlignerException extends PipelineException {

	private static final long serialVersionUID = 2121918036811689551L;

	public AlignerException(String message, String userMessage) {
		super(message, userMessage);
	}

}
