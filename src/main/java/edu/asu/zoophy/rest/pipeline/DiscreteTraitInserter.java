package edu.asu.zoophy.rest.pipeline;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Responsible for inserting discrete traits into BEAST input XML files
 * @author devdemetri
 */
public class DiscreteTraitInserter {
	
	private final String DOCUMENT_PATH;
	private final ZooPhyJob job;
	private Set<String> locations;
	private Document document;
	private Node beastNode;

	public DiscreteTraitInserter(ZooPhyJob job) throws TraitException {
		try {
			this.job = job;
			DOCUMENT_PATH = System.getProperty("user.dir")+"/ZooPhyJobs/"+job.getID()+".xml";
			locations = new HashSet<String>();
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			document = docBuilder.parse(DOCUMENT_PATH);
			beastNode = document.getElementsByTagName("beast").item(0);
		}
		catch (Exception e) {
			throw new TraitException("Error initializing DiscreteTraitInserter: "+e.getMessage(), null);
		}
	}

	/**
	 * Inserts locations as a discrete trait named States
	 */
	public void addLocation() throws TraitException {
		addTrait("states");
		saveChanges();
	}
	
	/**
	 * Adds the given trait to the BEAST input XML file
	 * @param traitName
	 * @throws TraitException
	 */
	private void addTrait(String traitName) throws TraitException {
		try {
			//I tried to make this block readable, but it was an especially convoluted process. Will refactor at some point for readability.
			String baseName = job.getID()+"-aligned";
			//add trait to taxa list
			int numTaxa = 0;
			NodeList taxa = document.getElementsByTagName("taxon");
			for (int i = 0; i < taxa.getLength(); i++) {
				Node taxon = taxa.item(i);
				NamedNodeMap taxaAttributes = taxon.getAttributes();
				Node taxaAttributesIdNode = taxaAttributes.getNamedItem("id");
				if (taxaAttributesIdNode != null) {
					numTaxa++;
					String idNodeContent = taxaAttributesIdNode.getTextContent();
					Element location = document.createElement("attr");
					location.setAttribute("name", traitName);
					String[] splits = idNodeContent.split("_");
					String geonameLocation = splits[splits.length-1];
					locations.add(geonameLocation);
					Node geoname = document.createTextNode(geonameLocation);
					location.appendChild(geoname);
					taxon.appendChild(location);
				}
			}
			int kValue = locations.size();
			int dimensionCalculation = kValue*(kValue-1);
			Comment numTaxaComment = document.createComment(" ntax="+numTaxa+" ");
			beastNode.insertBefore(numTaxaComment, taxa.item(0).getParentNode());
			int numChars = 0;
			Node align = document.getElementsByTagName("alignment").item(0);
			numChars = align.getFirstChild().getNextSibling().getTextContent().trim().length();
			Comment charComm = document.createComment(" ntax="+numTaxa+" nchar="+numChars+" ");
			beastNode.insertBefore(charComm, align);
			Comment startComment = document.createComment(" START Discrete Traits Model ");
			Comment endComment = document.createComment(" END Discrete Traits Model ");
			Comment genDataComment = document.createComment(" general data type for discrete trait model, '"+traitName+"' ");
			//add distinct states
			Element locationsNode = document.createElement("generalDataType");
			locationsNode.setAttribute("id", traitName+".dataType");
			Comment numStatesComment = document.createComment(" Number Of States = "+kValue+" ");
			locationsNode.appendChild(numStatesComment);
			for (String location : locations) {
				Element locationNode = document.createElement("state");
				locationNode.setAttribute("code", location);
				locationsNode.appendChild(locationNode);
			}
			//add attribute patterns
			Element attributePatterns = document.createElement("attributePatterns");
			attributePatterns.setAttribute("id", traitName+".pattern");
			attributePatterns.setAttribute("attribute", traitName);
			Element patternsTaxa = document.createElement("taxa");
			patternsTaxa.setAttribute("idref", "taxa");
			attributePatterns.appendChild(patternsTaxa);
			Element patternsGeneralDataType = document.createElement("generalDataType");
			patternsGeneralDataType.setAttribute("idref", traitName+".dataType");
			attributePatterns.appendChild(patternsGeneralDataType);
			Node constantSize = document.getElementsByTagName("constantSize").item(0);
			constantSize = constantSize.getPreviousSibling().getPreviousSibling().getPreviousSibling().getPreviousSibling();
			//insert states and patterns in proper place
			beastNode.insertBefore(locationsNode, constantSize);
			beastNode.insertBefore(attributePatterns, constantSize);
			beastNode.insertBefore(startComment, locationsNode);
			beastNode.insertBefore(genDataComment, locationsNode);
			Comment traitNameComment = document.createComment(" Data pattern for discrete trait, '"+traitName+"' ");
			beastNode.insertBefore(traitNameComment, attributePatterns);
			beastNode.insertBefore(endComment, constantSize);
			//add trait clock
			Element clockBranchRates = document.createElement("strictClockBranchRates");
			clockBranchRates.setAttribute("id", traitName+".branchRates");
			Element branchRate = document.createElement("rate");
			Element branchParameter = document.createElement("parameter");
			branchParameter.setAttribute("id", traitName+".clock.rate");
			branchParameter.setAttribute("value", "1.0");
			branchParameter.setAttribute("lower", "0.0");
			branchRate.appendChild(branchParameter);
			clockBranchRates.appendChild(branchRate);
			//add rare statistic
			Element rareStatistic = document.createElement("rateStatistic");
			rareStatistic.setAttribute("id", traitName+".meanRate");
			rareStatistic.setAttribute("name", traitName+".meanRate");
			rareStatistic.setAttribute("mode", "mean");
			rareStatistic.setAttribute("internal", "true");
			rareStatistic.setAttribute("external", "true");
			Element rareStatisticTreeModel = document.createElement("treeModel");
			rareStatisticTreeModel.setAttribute("idref", "treeModel");
			rareStatistic.appendChild(rareStatisticTreeModel);
			Element strictRates = document.createElement("strictClockBranchRates");
			strictRates.setAttribute("idref", traitName+".branchRates");
			rareStatistic.appendChild(strictRates);
			Comment clockComment = document.createComment(" The strict clock (Uniform rates across branches) ");
			//insert clock and rare statistic in correct place
			Node hkyModel = document.getElementsByTagName("HKYModel").item(0).getPreviousSibling().getPreviousSibling();
			beastNode.insertBefore(clockComment, hkyModel);
			beastNode.insertBefore(clockBranchRates, hkyModel);
			beastNode.insertBefore(rareStatistic, hkyModel);
			//start discrete trait models
			Queue<Node> modelsElements = new LinkedList<Node>();
			Element generalSubstitutionModel = document.createElement("generalSubstitutionModel");
			generalSubstitutionModel.setAttribute("id", traitName+".model");
			generalSubstitutionModel.setAttribute("randomizeIndicator", "false");
			Element substutionModelGeneralDataType = document.createElement("generalDataType");
			substutionModelGeneralDataType.setAttribute("idref", traitName+".dataType");
			generalSubstitutionModel.appendChild(substutionModelGeneralDataType);
			//frequencies
			Element substitutionFrequencies = document.createElement("frequencies");
			Element substitutionFrequencyModel = document.createElement("frequencyModel");
			substitutionFrequencyModel.setAttribute("id", traitName+".frequencyModel");
			substitutionFrequencyModel.setAttribute("normalize", "true");
			Element substitutionFrequencyGeneralDataType = document.createElement("generalDataType");
			substitutionFrequencyGeneralDataType.setAttribute("idref", traitName+".dataType");
			substitutionFrequencyModel.appendChild(substitutionFrequencyGeneralDataType);
			Element substitutionFrequencyModelFrequencies = document.createElement("frequencies");
			Element substitutionFrequencyModelFrequenciesParamater = document.createElement("parameter");
			substitutionFrequencyModelFrequenciesParamater.setAttribute("id", traitName+".frequencies");
			substitutionFrequencyModelFrequenciesParamater.setAttribute("dimension", String.valueOf(kValue));
			substitutionFrequencyModelFrequencies.appendChild(substitutionFrequencyModelFrequenciesParamater);
			substitutionFrequencyModel.appendChild(substitutionFrequencyModelFrequencies);
			substitutionFrequencies.appendChild(substitutionFrequencyModel);
			generalSubstitutionModel.appendChild(substitutionFrequencies);
			Comment ratesComment = document.createComment(" rates and indicators ");
			generalSubstitutionModel.appendChild(ratesComment);
			//rates and indicators
			Element generalSubstitutionRates = document.createElement("rates");
			Element generalSubstitutionRatesParameter = document.createElement("parameter");
			generalSubstitutionRatesParameter.setAttribute("id", traitName+".rates");
			generalSubstitutionRatesParameter.setAttribute("dimension", String.valueOf(dimensionCalculation));
			generalSubstitutionRatesParameter.setAttribute("value", "1.0");
			generalSubstitutionRatesParameter.setAttribute("lower", "0.0");
			generalSubstitutionRates.appendChild(generalSubstitutionRatesParameter);
			generalSubstitutionModel.appendChild(generalSubstitutionRates);
			Element rateIndicator = document.createElement("rateIndicator");
			Element rateIndicatorParameter = document.createElement("parameter");
			rateIndicatorParameter.setAttribute("id", traitName+".indicators");
			rateIndicatorParameter.setAttribute("dimension", String.valueOf(dimensionCalculation));
			rateIndicatorParameter.setAttribute("value", "1.0");
			rateIndicator.appendChild(rateIndicatorParameter);
			generalSubstitutionModel.appendChild(rateIndicator);
			modelsElements.add(startComment.cloneNode(true));
			Comment asymmComment = document.createComment(" asymmetric CTMC model for discrete state reconstructions ");
			modelsElements.add(asymmComment);
			modelsElements.add(generalSubstitutionModel);
			//sum statistic
			Element sumStatistic = document.createElement("sumStatistic");
			sumStatistic.setAttribute("id", traitName+".nonZeroRates");
			sumStatistic.setAttribute("elementwise", "true");
			Element sumParameter = document.createElement("parameter");
			sumParameter.setAttribute("idref", traitName+".indicators");
			sumStatistic.appendChild(sumParameter);
			modelsElements.add(sumStatistic);
			//product statistic
			Element productStatistic = document.createElement("productStatistic");
			productStatistic.setAttribute("id", traitName+".actualRates");
			productStatistic.setAttribute("elementwise", "false");
			Element productStatisticIndicatorsParameter = document.createElement("parameter");
			productStatisticIndicatorsParameter.setAttribute("idref", traitName+".indicators");
			productStatistic.appendChild(productStatisticIndicatorsParameter);
			Element productStatisticRatesParameter = document.createElement("parameter");
			productStatisticRatesParameter.setAttribute("idref", traitName+".rates");
			productStatistic.appendChild(productStatisticRatesParameter);
			modelsElements.add(productStatistic);
			//site model
			Element siteModel = document.createElement("siteModel");
			siteModel.setAttribute("id", traitName+".siteModel");
			Element siteSubstitutionModel = document.createElement("substitutionModel");
			Element siteGeneralSubstitutionModel = document.createElement("generalSubstitutionModel");
			siteGeneralSubstitutionModel.setAttribute("idref", traitName+".model");
			siteSubstitutionModel.appendChild(siteGeneralSubstitutionModel);
			siteModel.appendChild(siteSubstitutionModel);
			modelsElements.add(siteModel);
			Comment ancestralTreeComment = document.createComment(" Likelihood for tree given discrete trait data ");
			modelsElements.add(ancestralTreeComment);
			//ancestral tree
			Element ancestralTree = document.createElement("ancestralTreeLikelihood");
			ancestralTree.setAttribute("id", traitName+".treeLikelihood");
			ancestralTree.setAttribute("stateTagName", traitName+".states");
			Element ancestralTreeAttributePatterns = document.createElement("attributePatterns");
			ancestralTreeAttributePatterns.setAttribute("idref", traitName+".pattern");
			ancestralTree.appendChild(ancestralTreeAttributePatterns);
			Element ancestralTreeTreeModel = document.createElement("treeModel");
			ancestralTreeTreeModel.setAttribute("idref", "treeModel");
			ancestralTree.appendChild(ancestralTreeTreeModel);
			Element ancestralTreeSiteModel = document.createElement("siteModel");
			ancestralTreeSiteModel.setAttribute("idref", traitName+".siteModel");
			ancestralTree.appendChild(ancestralTreeSiteModel);
			Element ancestralTreeGeneralSubstitutionModel = document.createElement("generalSubstitutionModel");
			ancestralTreeGeneralSubstitutionModel.setAttribute("idref", traitName+".model");
			ancestralTree.appendChild(ancestralTreeGeneralSubstitutionModel);
			Element ancestralTreeBranchRates = document.createElement("strictClockBranchRates");
			ancestralTreeBranchRates.setAttribute("idref", traitName+".branchRates");
			ancestralTree.appendChild(ancestralTreeBranchRates);
			Comment rootStateComment = document.createComment(" The root state frequencies ");
			ancestralTree.appendChild(rootStateComment);
			//frequency model//
			Element ancestralTreeFrequencyModel = document.createElement("frequencyModel");
			ancestralTreeFrequencyModel.setAttribute("id", traitName+".root.frequencyModel");
			ancestralTreeFrequencyModel.setAttribute("normalize", "true");
			Element ancestralTreeFrequencyGeneralDataType = document.createElement("generalDataType");
			ancestralTreeFrequencyGeneralDataType.setAttribute("idref", traitName+".dataType");
			ancestralTreeFrequencyModel.appendChild(ancestralTreeFrequencyGeneralDataType);
			Element ancestralTreeFrequenciesModelFrequencies = document.createElement("frequencies");
			Element ancestralTreeFrequenciesModelFrequenciesParameter = document.createElement("parameter");
			ancestralTreeFrequenciesModelFrequenciesParameter.setAttribute("id", traitName+".root.frequencies");
			ancestralTreeFrequenciesModelFrequenciesParameter.setAttribute("dimension", String.valueOf(kValue));
			ancestralTreeFrequenciesModelFrequencies.appendChild(ancestralTreeFrequenciesModelFrequenciesParameter);
			ancestralTreeFrequencyModel.appendChild(ancestralTreeFrequenciesModelFrequencies);
			ancestralTree.appendChild(ancestralTreeFrequencyModel);
			modelsElements.add(ancestralTree);
			modelsElements.add(endComment.cloneNode(true));
			//insert before the operators section
			Node operators = document.getElementsByTagName("operators").item(0).getPreviousSibling().getPreviousSibling();
			while (!modelsElements.isEmpty()) {
				beastNode.insertBefore(modelsElements.remove(), operators);
			}
			//add scale operators//
			operators = operators.getNextSibling().getNextSibling();
			Node subtreeSlide = document.getElementsByTagName("subtreeSlide").item(0);
			Element operatorsScaleOperator = document.createElement("scaleOperator");
			operatorsScaleOperator.setAttribute("weight", "3");
			operatorsScaleOperator.setAttribute("scaleFactor", "0.75");
			Element operatorsScaleParameter = document.createElement("parameter");
			operatorsScaleParameter.setAttribute("idref", traitName+".clock.rate");
			operatorsScaleOperator.appendChild(operatorsScaleParameter);
			operators.insertBefore(operatorsScaleOperator, subtreeSlide);
			//add up down operator//
			Element upDownOperator = document.createElement("upDownOperator");
			upDownOperator.setAttribute("scaleFactor", "0.75");
			upDownOperator.setAttribute("weight", "3");
			Element up = document.createElement("up");
			Element upParameter = document.createElement("parameter");
			upParameter.setAttribute("idref", traitName+".clock.rate");
			up.appendChild(upParameter);
			Element down = document.createElement("down");
			Element downParameter = document.createElement("parameter");
			downParameter.setAttribute("idref", "treeModel.allInternalNodeHeights");
			down.appendChild(downParameter);
			upDownOperator.appendChild(up);
			upDownOperator.appendChild(down);
			operators.appendChild(upDownOperator);
			//scale operator 2//
			Element scaleOperator = document.createElement("scaleOperator");
			scaleOperator.setAttribute("scaleFactor", "0.75");
			scaleOperator.setAttribute("weight", "15");
			scaleOperator.setAttribute("scaleAllIndependently", "true");
			Element scaleOperatorParameter = document.createElement("parameter");
			scaleOperatorParameter.setAttribute("idref", traitName+".rates");
			scaleOperator.appendChild(scaleOperatorParameter);
			operators.appendChild(scaleOperator);
			//bit flip//
			Element bitFlipOperator = document.createElement("bitFlipOperator");
			bitFlipOperator.setAttribute("weight", "7");
			Element bitFlipParameter = document.createElement("parameter");
			bitFlipParameter.setAttribute("idref", traitName+".indicators");
			bitFlipOperator.appendChild(bitFlipParameter);
			operators.appendChild(bitFlipOperator);
			//delta exchange//
			Element deltaExchange = document.createElement("deltaExchange");
			deltaExchange.setAttribute("delta", "0.75");
			deltaExchange.setAttribute("weight", "1");
			Element deltaExchangeParameter = document.createElement("parameter");
			deltaExchangeParameter.setAttribute("idref", traitName+".root.frequencies");
			deltaExchange.appendChild(deltaExchangeParameter);
			operators.appendChild(deltaExchange);
			//mcmc stuff//
			Element ctmcScalePrior = document.createElement("ctmcScalePrior");
			Element ctmcScale = document.createElement("ctmcScale");
			Element ctmcParameter = document.createElement("parameter");
			ctmcParameter.setAttribute("idref", traitName+".clock.rate");
			ctmcScale.appendChild(ctmcParameter);
			ctmcScalePrior.appendChild(ctmcScale);
			Element ctmcTreeModel = document.createElement("treeModel");
			ctmcTreeModel.setAttribute("idref", "treeModel");
			ctmcScalePrior.appendChild(ctmcTreeModel);
			Node oneOnXPrior = document.getElementsByTagName("oneOnXPrior").item(0);
			Node oneOnXPriorPrior = oneOnXPrior.getParentNode();
			oneOnXPriorPrior.insertBefore(ctmcScalePrior, oneOnXPrior);
			//poisson//
			Element poissonPrior = document.createElement("poissonPrior");
			int poissonOffset = kValue-1;
			poissonPrior.setAttribute("mean", poissonOffset+".0");
			poissonPrior.setAttribute("offset", "0.0");
			Element poissStatistic = document.createElement("statistic");
			poissStatistic.setAttribute("idref", traitName+".nonZeroRates");
			poissonPrior.appendChild(poissStatistic);
			Node coalescentPrior = oneOnXPrior.getNextSibling().getNextSibling();
			oneOnXPriorPrior.insertBefore(poissonPrior, coalescentPrior);
			//uniform//
			Element uniformPrior = document.createElement("uniformPrior");
			uniformPrior.setAttribute("lower", "0.0");
			uniformPrior.setAttribute("upper", "1.0");
			Element uniformParam = document.createElement("parameter");
			uniformParam.setAttribute("idref", traitName+".frequencies");
			uniformPrior.appendChild(uniformParam);
			oneOnXPriorPrior.insertBefore(uniformPrior, coalescentPrior);
			//cached//
			Element cachedPrior = document.createElement("cachedPrior");
			Element cachedParameter = document.createElement("parameter");
			cachedParameter.setAttribute("idref", traitName+".rates");
			Node gammaParameter = cachedParameter.cloneNode(false);
			Element gammaPrior = document.createElement("gammaPrior");
			gammaPrior.setAttribute("shape", "1.0");
			gammaPrior.setAttribute("scale", "1.0");
			gammaPrior.setAttribute("offset", "0.0");
			gammaPrior.appendChild(gammaParameter);
			cachedPrior.appendChild(gammaPrior);
			cachedPrior.appendChild(cachedParameter);
			oneOnXPriorPrior.insertBefore(cachedPrior, coalescentPrior);
			//uniform2//
			Node uniformPrior2 = uniformPrior.cloneNode(false);
			Element uniformPrior2Parameter = (Element) uniformParam.cloneNode(false);
			uniformPrior2Parameter.setAttribute("idref", traitName+".root.frequencies");
			uniformPrior2.appendChild(uniformPrior2Parameter);
			oneOnXPriorPrior.insertBefore(uniformPrior2, coalescentPrior);
			//scbr//
			Element strictClockBranchRatesPrior = document.createElement("strictClockBranchRates");
			strictClockBranchRatesPrior.setAttribute("idref", traitName+".branchRates");
			oneOnXPriorPrior.appendChild(strictClockBranchRatesPrior);
			oneOnXPriorPrior.appendChild(startComment.cloneNode(true));
			oneOnXPriorPrior.appendChild(siteGeneralSubstitutionModel.cloneNode(false));
			oneOnXPriorPrior.appendChild(endComment.cloneNode(true));
			//likelihood//
			Node likelihood = oneOnXPriorPrior.getNextSibling().getNextSibling();
			likelihood.appendChild(startComment.cloneNode(true));
			Element ancestralTreeClone = (Element) ancestralTree.cloneNode(false);
			ancestralTreeClone.removeAttribute("id");
			ancestralTreeClone.setAttribute("idref", traitName+".treeLikelihood");
			ancestralTreeClone.removeAttribute("stateTagName");
			likelihood.appendChild(ancestralTreeClone);
			likelihood.appendChild(endComment.cloneNode(true));
			//logs//
			NodeList logs = document.getElementsByTagName("log");
			Node screenLog = logs.item(0);
			//columns//
			Element clockColumn = document.createElement("column");
			clockColumn.setAttribute("label", traitName+".clock.rate");
			clockColumn.setAttribute("sf", "6");
			clockColumn.setAttribute("width", "12");
			Element clockColumnParameter = document.createElement("parameter");
			clockColumnParameter.setAttribute("idref", traitName+".clock.rate");
			clockColumn.appendChild(clockColumnParameter);
			screenLog.appendChild(clockColumn);
			screenLog.appendChild(startComment.cloneNode(true));
			Element rateColumn = (Element) clockColumn.cloneNode(false);
			rateColumn.setAttribute("label", traitName+".nonZeroRates");
			Element rateColumnStatistic = (Element) sumStatistic.cloneNode(false);
			rateColumnStatistic.removeAttribute("id");
			rateColumnStatistic.setAttribute("idref", traitName+".nonZeroRates");
			rateColumnStatistic.removeAttribute("elementwise");
			rateColumn.appendChild(rateColumnStatistic);
			screenLog.appendChild(rateColumn);
			screenLog.appendChild(endComment.cloneNode(true));
			//file log//
			Node fileLog = logs.item(1);
			Node clockRateParam = operatorsScaleParameter.cloneNode(false);
			Node currSpot = fileLog.getChildNodes().item(17);//rateStatistic
			fileLog.insertBefore(clockRateParam, currSpot);
			Element geoRateStat = (Element) currSpot.cloneNode(false);
			geoRateStat.setAttribute("idref", traitName+".meanRate");
			currSpot = currSpot.getNextSibling().getNextSibling();
			fileLog.insertBefore(geoRateStat, currSpot);
			fileLog.insertBefore(startComment.cloneNode(true), currSpot);
			fileLog.insertBefore(productStatisticRatesParameter.cloneNode(false), currSpot);
			fileLog.insertBefore(sumParameter.cloneNode(false), currSpot);
			fileLog.insertBefore(rateColumnStatistic.cloneNode(false), currSpot);
			fileLog.insertBefore(endComment.cloneNode(true), currSpot);
			currSpot = currSpot.getNextSibling().getNextSibling();
			currSpot = currSpot.getNextSibling().getNextSibling();
			fileLog.insertBefore(ancestralTreeBranchRates.cloneNode(true), currSpot);
			fileLog.insertBefore(startComment.cloneNode(true), currSpot);
			fileLog.insertBefore(ancestralTreeClone.cloneNode(true), currSpot);
			fileLog.insertBefore(endComment.cloneNode(true), currSpot);
			//trait log//
			Node treeLog = fileLog.getNextSibling().getNextSibling();
			Node mcmc = treeLog.getParentNode();
			mcmc.insertBefore(startComment.cloneNode(true), treeLog);
			Element traitLog = document.createElement("log");
			traitLog.setAttribute("id", baseName+"."+traitName+"rateMatrixLog");
			traitLog.setAttribute("logEvery", "1000");
			traitLog.setAttribute("fileName", baseName+"."+traitName+".rates.log");
			traitLog.appendChild(productStatisticRatesParameter.cloneNode(false));
			traitLog.appendChild(sumParameter.cloneNode(false));
			traitLog.appendChild(rateColumnStatistic.cloneNode(false));
			mcmc.insertBefore(traitLog, treeLog);
			mcmc.insertBefore(endComment.cloneNode(true), treeLog);
			//tree log//
			treeLog = treeLog.getNextSibling().getNextSibling();
			currSpot = treeLog.getChildNodes().item(5);
			Element logTrait = document.createElement("trait");
			logTrait.setAttribute("name", "rate");
			logTrait.setAttribute("tag", traitName+".rate");
			logTrait.appendChild(strictRates.cloneNode(false));
			treeLog.insertBefore(logTrait, currSpot);
			treeLog.appendChild(document.createComment(" START Ancestral state reconstruction "));
			Element trait = document.createElement("trait");
			trait.setAttribute("name", traitName+".states");
			trait.setAttribute("tag", traitName);
			trait.appendChild(ancestralTreeClone.cloneNode(false));
			treeLog.appendChild(trait);
			treeLog.appendChild(document.createComment(" END Ancestral state reconstruction "));
		}
		catch (Exception e) {
			throw new TraitException("ERROR adding trait: "+traitName+" : "+e.getMessage(), null);
		}
	}

	private void saveChanges() throws TraitException {
		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(new File(DOCUMENT_PATH));
			transformer.transform(source, result);
			beastNode = null;
			document = null;
		} 
		catch (Exception e) {
			throw new TraitException("ERROR could not save updated XML: "+e.getMessage(), null);
		}
	}

}
