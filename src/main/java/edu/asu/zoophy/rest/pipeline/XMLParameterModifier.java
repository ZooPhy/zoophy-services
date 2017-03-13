package edu.asu.zoophy.rest.pipeline;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Modifies BEAST XML parameters to reflect user options
 * @author devdemetri
 */
public class XMLParameterModifier {

	private Logger log;
	
	public XMLParameterModifier(File beastXML) throws XMLParameterException {
		log = Logger.getLogger("XMLParameterModifier");
		if (beastXML != null && beastXML.exists() && !beastXML.isDirectory()) {
			try {
				log.info("Initializing XML Parameter Modifier...");
				//TODO: load in XML and set key nodes
				log.info("XML Parameter Modifier initialized.");
			}
			catch (Exception e) {
				log.log(Level.SEVERE, "Error setting up XML Parameter Modifier: "+e.getMessage());
				throw new XMLParameterException("Error setting up XML Parameter Modifier: "+e.getMessage(), "Error setting up XML Parameter Modifier.");
			}
		}
		else {
			log.log(Level.SEVERE, "ERROR BEAST XML file does not exist: "+beastXML.getAbsolutePath());
			throw new XMLParameterException("BEAST XML file does not exist: "+beastXML.getAbsolutePath(), "Error setting up XML Parameter Modifier.");
		}
	}
	
	/**
	 * Modifies the BEAST input XML to reflect custom parameters
	 * @param customParameters
	 * @throws XMLParameterException
	 */
	public void setCustomXMLParameters(XMLParameters customParameters) throws XMLParameterException {
		try {
			final XMLParameters defaultParamters = XMLParameters.getDefault();
			log.info("Setting custom XML Parameters...");
			if (defaultParamters.getChainLength() != customParameters.getChainLength()) {
				log.info("Setting custom Chain Length: "+customParameters.getChainLength());
				setChainLength(customParameters.getChainLength());
			}
			else {
				log.info("Default Chain Length selected.");
			}
			if (defaultParamters.getSubSampleRate() != customParameters.getSubSampleRate()) {
				log.info("Setting custom Sub Sampling Rate: "+customParameters.getSubSampleRate());
				setSubSamplingRate(customParameters.getSubSampleRate());
			}
			else {
				log.info("Default Sub Sampling Rate selected.");
			}
			if (defaultParamters.getSubstitutionModel() != customParameters.getSubstitutionModel()) {
				log.info("Setting custom Substitution Model: "+customParameters.getSubstitutionModel().toString());
				setSubstitutionModel(customParameters.getSubstitutionModel());
			}
			else {
				log.info("Default Substitution Model selected.");
			}
			log.info("Custom XML Parameters set.");
		}
		catch (XMLParameterException xmlpe) {
			log.log(Level.SEVERE, "Error setting custom XML Parameters: "+xmlpe.getMessage());
			throw xmlpe;
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Error setting custom XML Parameters: "+e.getMessage());
			throw new XMLParameterException("Error setting custom XML Parameters: "+e.getMessage(), "Error setting custom XML Parameters.");
		}
	}
	
	/**
	 * Modifies the BEAST input XML Chain Length
	 * @param chainLength
	 * @throws XMLParameterException
	 */
	private void setChainLength(int chainLength) throws XMLParameterException {
		try {
			log.info("Setting custom Chain Length...");
			//TODO: set chain length
			log.info("Custom Chain Length set.");
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Error setting custom Chain Length: "+e.getMessage());
			throw new XMLParameterException("Error setting custom Chain Length: "+e.getMessage(), "Error setting custom Chain Length.");
		}
	}
	
	/**
	 * Modifies the BEAST input XML Sub Sampling Rate
	 * @param subSamplingRate
	 * @throws XMLParameterException
	 */
	private void setSubSamplingRate(int subSamplingRate) throws XMLParameterException {
		try {
			log.info("Setting custom Sub Sampling Rate...");
			//TODO: set sub sampling rate
			log.info("Custom Sub Sampling Rate set.");
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Error setting custom Sub Sampling Rate: "+e.getMessage());
			throw new XMLParameterException("Error setting custom Sub Sampling Rate: "+e.getMessage(), "Error setting custom Sub Sampling Rate.");
		}
	}
	
	/**
	 * Modifies the BEAST input XML Substitution Model
	 * @param beastSubstitutionModel
	 * @throws XMLParameterException
	 */
	private void setSubstitutionModel(BEASTSubstitutionModel beastSubstitutionModel) throws XMLParameterException {
		//TODO: may need to use different BeastGen template instead of altering XML afterwards 
		try {
			log.info("Setting custom Substitution Model...");
			//TODO: set substitution model
			log.info("Custom Substitution Model set.");
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Error setting custom Substitution Model: "+e.getMessage());
			throw new XMLParameterException("Error setting custom Substitution Model: "+e.getMessage(), "Error setting custom Substitution Model.");
		}
	}
	
}
