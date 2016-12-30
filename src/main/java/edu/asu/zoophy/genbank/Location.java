package edu.asu.zoophy.genbank;

/**
 * Virus location
 * @author devdemetri
 */
public class Location {
	
	private Long geonameID;
	private String accession;
	private String location;
	private Double latitude;
	private Double longitude;
	private String geonameType;
	private String country;
	
	public Location() {
		
	}
	
	public Long getGeonameID() {
		return geonameID;
	}
	
	public void setGeonameID(Long geonameID) {
		this.geonameID = geonameID;
	}
	
	public String getAccession() {
		return accession;
	}
	
	public void setAccession(String accession) {
		this.accession = accession;
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
	
	public String getGeonameType() {
		return geonameType;
	}
	
	public void setGeonameType(String geonameType) {
		this.geonameType = geonameType;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}
	
}
