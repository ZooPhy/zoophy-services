package edu.asu.zoophy.rest.pipeline.glm;

/**
 * Predictor values for a generic Location,
 * @author devdemetri
 */
public class LocationPredictor {

	private Double latitude;
	private Double longitude;
	private int sampleSize;
	
	public LocationPredictor() {
		sampleSize = 0;
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

	public int getSampleSize() {
		return sampleSize;
	}

	public void setSampleSize(int sampleSize) {
		this.sampleSize = sampleSize;
	}
	
	/**
	 * Increment SampleSize Predictor
	 */
	public void addSample() {
		this.sampleSize++;
	}
	
}
