package edu.asu.zoophy.pipeline;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception for ZooPhy pipeline errors
 * @author devdemetri
 */
@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR)
public class PipelineException extends Exception {

	private static final long serialVersionUID = 6458553446907916178L;
	private String userMessage;
	
	public PipelineException(String msg, String userMsg) {
		super(msg);
		userMessage = userMsg;
	}

	public String getUserMessage() {
		return userMessage;
	}

}
