package edu.asu.zoophy.rest.pipeline.utils;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import edu.asu.zoophy.rest.pipeline.PipelineException;

/**
 * Custom exception for GeoHierarchy errors
 * @author devdemetri
 */
@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR)
public class GeoHierarchyException extends PipelineException {

	private static final long serialVersionUID = -4000750562500618508L;

	public GeoHierarchyException(String message, String userMessage) {
		super(message, userMessage);
	}

}
