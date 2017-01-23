package edu.asu.zoophy.rest.pipeline.glm;

/**
 * Specific Predictor values for a specific state, that have been normalized and/or averaged as necessary 
 * @author devdemetri
 */
public class StatePredictor {

	private Double latitude;
	private Double longitude;
	private Double elevation;
	private Double temperature;
	private Double averagePopulation;
	private Double averageMedianAge;
	
	public StatePredictor() {
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public Double getElevation() {
		return elevation;
	}

	public void setElevation(Double elevation) {
		this.elevation = elevation;
	}

	public Double getTemperature() {
		return temperature;
	}

	public void setTemperature(Double temperature) {
		this.temperature = temperature;
	}

	public Double getAveragePopulation() {
		return averagePopulation;
	}

	public void setAveragePopulation(Double averagePopulation) {
		this.averagePopulation = averagePopulation;
	}

	public Double getAverageMedianAge() {
		return averageMedianAge;
	}

	public void setAverageMedianAge(Double averageMedianAge) {
		this.averageMedianAge = averageMedianAge;
	}
	
}
