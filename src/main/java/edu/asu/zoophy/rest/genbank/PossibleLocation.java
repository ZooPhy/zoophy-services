package edu.asu.zoophy.rest.genbank;

/**
 * Virus' possible location
 * @author amagge
 */
public class PossibleLocation {
	
	private Long geonameID;
	private String location;
	private Double latitude;
	private Double longitude;
	private Double probability;
	
	public PossibleLocation() {
		
	}
	
	public Long getGeonameID() {
		return geonameID;
	}
	
	public void setGeonameID(Long geonameID) {
		this.geonameID = geonameID;
	}
	
	public String getLocation() {
		return location;
	}
	
	public void setLocation(String location) {
		this.location = location;
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

	public Double getProbability() {
		return probability;
	}

	public void setProbability(Double probability) {
		this.probability = probability;
	}
	
}
