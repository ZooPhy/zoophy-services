package edu.asu.zoophy.rest.pipeline;

/**
 * Custom BEAST XML parameters for ZooPhy jobs
 * @author devdemetri
 */
public class XMLParameters {

	private boolean isDefault = false;
	private Integer chainLength = null;
	private BEASTSubstitutionModel substitutionModel = null;
	
	private static XMLParameters defaultParams = null;
	
	public XMLParameters() {
		
	}
	
	/**
	 * @return Default XML Parameters
	 */
	public static XMLParameters getDefault() {
		if (defaultParams == null) {
			defaultParams = new XMLParameters();
			defaultParams.setChainLength(10000000);
			defaultParams.setSubstitutionModel(BEASTSubstitutionModel.HKY);
			defaultParams.isDefault = true;
		}
		return defaultParams;
	}

	public Integer getChainLength() {
		return chainLength;
	}

	public void setChainLength(int chainLength) {
		this.chainLength = chainLength;
	}
	
	public boolean isDefault() {
		return isDefault;
	}

	public BEASTSubstitutionModel getSubstitutionModel() {
		return substitutionModel;
	}

	public void setSubstitutionModel(BEASTSubstitutionModel substitutionModel) {
		this.substitutionModel = substitutionModel;
	}
	
}
