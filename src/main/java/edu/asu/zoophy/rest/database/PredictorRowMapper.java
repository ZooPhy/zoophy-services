package edu.asu.zoophy.rest.database;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import edu.asu.zoophy.rest.pipeline.glm.Predictor;

/**
 * Maps SQL row to GLM Predictor
 * @author devdemetri
 */
public class PredictorRowMapper implements RowMapper<Predictor> {

	@Override
	public Predictor mapRow(ResultSet row, int rowNum) throws SQLException {
		Predictor predictor = new Predictor();
		predictor.setName(row.getString("Key"));
		predictor.setState(row.getString("State"));
		predictor.setValue(row.getDouble("Value"));
		predictor.setYear(row.getInt("Year"));
		return predictor;
	}

}
