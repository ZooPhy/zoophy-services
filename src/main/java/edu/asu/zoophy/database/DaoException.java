package edu.asu.zoophy.database;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception for ZooPhyDAO errors
 * @author devdemetri
 */
@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR)
public class DaoException extends Exception {

	private static final long serialVersionUID = 6604079125536697161L;
	
	public DaoException(String message) {
		super(message);
	}

}
