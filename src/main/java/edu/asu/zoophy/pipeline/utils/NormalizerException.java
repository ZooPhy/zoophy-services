package edu.asu.zoophy.pipeline.utils;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import edu.asu.zoophy.pipeline.PipelineException;

/**
 * Custom exception for Pipeline Normalizer errors
 * @author devdemetri
 */
@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR)
public class NormalizerException extends PipelineException {

	private static final long serialVersionUID = 4343167902900185061L;

	public NormalizerException(String message, String userMessage) {
		super(message, userMessage);
	}

}
