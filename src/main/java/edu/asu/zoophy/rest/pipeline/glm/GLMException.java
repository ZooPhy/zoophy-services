package edu.asu.zoophy.rest.pipeline.glm;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import edu.asu.zoophy.rest.pipeline.PipelineException;
/**
 * Custom exception for GLM errors
 * @author devdemetri
 */
@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR)
public class GLMException extends PipelineException {

	private static final long serialVersionUID = -6513152109470129742L;

	public GLMException(String message, String userMessage) {
		super(message, userMessage);
	}

}
