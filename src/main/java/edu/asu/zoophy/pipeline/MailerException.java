package edu.asu.zoophy.pipeline;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception for Pipeline Email errors
 * @author devdemetri
 */
@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR)
public class MailerException extends PipelineException {

	private static final long serialVersionUID = -5433642930007356206L;

	public MailerException(String msg, String userMsg) {
		super(msg, userMsg);
	}

}
