package edu.asu.zoophy.rest.pipeline.glm;

/**
 * GLM Predictor for BEAST runs
 * @author devdemetri
 */
public class Predictor {
	
	private String state;
	private String name;
	private Double value;
	private Integer year;
	
	public Predictor() {
		
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public Double getValue() {
		return value;
	}

	public void setValue(Double value) {
		this.value = value;
	}

	public Integer getYear() {
		return year;
	}

	public void setYear(Integer year) {
		this.year = year;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
}
