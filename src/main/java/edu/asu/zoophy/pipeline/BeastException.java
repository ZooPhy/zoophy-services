package edu.asu.zoophy.pipeline;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception for BEAST process errors
 * @author devdemetri
 */
@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR)
public class BeastException extends PipelineException {

	private static final long serialVersionUID = -608485967306011297L;

	public BeastException(String message, String userMessage) {
		super(message, userMessage);
	}

}
