package edu.asu.zoophy.rest.pipeline;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Modifies BEAST XML parameters to reflect user options
 * @author devdemetri
 */
public class XMLParameterModifier {

	private Logger log;
	private Document document;
	private final String DOCUMENT_PATH;
	private final Node beastNode;
	
	public XMLParameterModifier(File beastXML) throws XMLParameterException {
		log = Logger.getLogger("XMLParameterModifier");
		if (beastXML != null && beastXML.exists() && !beastXML.isDirectory()) {
			try {
				log.info("Initializing XML Parameter Modifier...");
				DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
				DOCUMENT_PATH = beastXML.getAbsolutePath();
				document = docBuilder.parse(DOCUMENT_PATH);
				beastNode = document.getElementsByTagName("beast").item(0);
				log.info("XML Parameter Modifier initialized.");
				if (beastNode == null) {
					log.log(Level.SEVERE, "Failed to load XML document: "+DOCUMENT_PATH);
					throw new XMLParameterException("Failed to load XML document: "+DOCUMENT_PATH, "Failed to load XML Document");
				}
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
	 * Modifies the BEAST input XML to reflect custom parameters.
	 * Can only be called once per XMLParameterModifier instance.
	 * @param customParameters
	 * @throws XMLParameterException
	 */
	public void setCustomXMLParameters(XMLParameters customParameters) throws XMLParameterException {
		if (document == null) {
			log.log(Level.SEVERE, "Error setting custom XML Parameters: NULL document.");
			throw new XMLParameterException("Error setting custom XML Parameters: NULL document.", "Error setting custom XML Parameters.");
		}
		try {
			final XMLParameters defaultParamters = XMLParameters.getDefault();
			log.info("Setting custom XML Parameters...");
			if (defaultParamters.getChainLength().intValue() != customParameters.getChainLength().intValue()) {
				log.info("Setting custom Chain Length: "+customParameters.getChainLength());
				setChainLength(customParameters.getChainLength());
			}
			else {
				log.info("Default Chain Length selected.");
			}
			if (defaultParamters.getSubSampleRate().intValue() != customParameters.getSubSampleRate().intValue()) {
				log.info("Setting custom Sub Sampling Rate: "+customParameters.getSubSampleRate());
				setSubSamplingRate(customParameters.getSubSampleRate());
			}
			else {
				log.info("Default Sub Sampling Rate selected.");
			}
			DiscreteTraitInserter.saveChanges(document, DOCUMENT_PATH);
			document = null;
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
			final String targetAttributeName = "chainLength";
			log.info("Setting custom Chain Length...");
			Element mcmc = (Element) document.getElementsByTagName("mcmc").item(0);
			mcmc.setAttribute(targetAttributeName, String.valueOf(chainLength));
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
			final String targetAttributeName = "logEvery";
			log.info("Setting custom Sub Sampling Rate...");
			Element screenLog = (Element) document.getElementsByTagName("log").item(0);
			screenLog.setAttribute(targetAttributeName, String.valueOf(subSamplingRate));
			Element fileLog = (Element) document.getElementsByTagName("log").item(1);
			fileLog.setAttribute(targetAttributeName, String.valueOf(subSamplingRate));
			Element treeFileLog = (Element) document.getElementsByTagName("logTree").item(0);
			treeFileLog.setAttribute(targetAttributeName, String.valueOf(subSamplingRate));
			log.info("Custom Sub Sampling Rate set.");
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Error setting custom Sub Sampling Rate: "+e.getMessage());
			throw new XMLParameterException("Error setting custom Sub Sampling Rate: "+e.getMessage(), "Error setting custom Sub Sampling Rate.");
		}
	}
	
}
