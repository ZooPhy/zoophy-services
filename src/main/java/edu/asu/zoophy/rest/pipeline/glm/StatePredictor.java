package edu.asu.zoophy.rest.pipeline.glm;

/**
 * Extension of LocationPredicotr for specific state predictors that have been normalized and/or averaged as necessary.
 * @author devdemetri
 */
public class StatePredictor extends LocationPredictor {

	private Double elevation;
	private Double temperature;
	private Double averagePopulation;
	private Double averageMedianAge;
	
	public StatePredictor() {
		super();
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
