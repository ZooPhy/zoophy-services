package edu.asu.zoophy.pipeline.utils;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

/**
 * Representation of Geoname Type Hierarchy for the purpose of ensuring disjoint locations
 * @author devdemetri
 */
public class GeoHierarchy {

	private class HierarchyNode {
		
		protected String type;
		protected Set<HierarchyNode> parents;
		
		protected HierarchyNode(String type) {
			this.type = type;
			parents = new HashSet<HierarchyNode>();
		}
		
	}
	
	private static GeoHierarchy hierarchy = null;
	private static HierarchyNode root = null;
	private static Map<String,HierarchyNode> nodeMap = new HashMap<String,HierarchyNode>();
	
	private GeoHierarchy() {
		root = new HierarchyNode("CONT");
		nodeMap.put(root.type,root);
		//political entities//
		HierarchyNode pcli = new HierarchyNode("PCLI");
		pcli.parents.add(root);
		nodeMap.put(pcli.type,pcli);
		HierarchyNode pcl = new HierarchyNode("PCL");
		pcl.parents.add(root);
		nodeMap.put(pcl.type,pcl);
		HierarchyNode pclh = new HierarchyNode("PCLH");
		pclh.parents.add(root);
		nodeMap.put(pclh.type,pclh);
		HierarchyNode pcld = new HierarchyNode("PCLD");
		pcld.parents.add(root);
		nodeMap.put(pcld.type,pcld);
		//adms//
		HierarchyNode adm1 = new HierarchyNode("ADM1");
		adm1.parents.add(pcli);
		adm1.parents.add(pcl);
		adm1.parents.add(pclh);
		adm1.parents.add(pcld);
		nodeMap.put(adm1.type,adm1);
		HierarchyNode adm1h = new HierarchyNode("ADM1H");
		adm1h.parents.add(pcli);
		adm1h.parents.add(pcl);
		adm1h.parents.add(pclh);
		adm1h.parents.add(pcld);
		nodeMap.put(adm1h.type,adm1h);
		HierarchyNode admd = new HierarchyNode("ADMD");
		admd.parents.add(pcli);
		admd.parents.add(pcl);
		admd.parents.add(pclh);
		admd.parents.add(pcld);
		nodeMap.put(admd.type,admd);
		HierarchyNode adm2 = new HierarchyNode("ADM2");
		adm2.parents.add(adm1);
		adm2.parents.add(adm1h);
		adm2.parents.add(admd);
		nodeMap.put(adm2.type,adm2);
		HierarchyNode adm2h = new HierarchyNode("ADM2H");
		adm2h.parents.add(adm1);
		adm2h.parents.add(adm1h);
		adm2h.parents.add(admd);
		nodeMap.put(adm2h.type,adm2h);
		HierarchyNode adm3 = new HierarchyNode("ADM3");
		adm3.parents.add(adm2);
		adm3.parents.add(adm2h);
		nodeMap.put(adm3.type,adm3);
		HierarchyNode adm4 = new HierarchyNode("ADM4");
		adm4.parents.add(adm3);
		nodeMap.put(adm4.type,adm4);
		//Populated places//
		HierarchyNode ppl = new HierarchyNode("PPL");
		ppl.parents.add(adm4);
		nodeMap.put(ppl.type, ppl);
		HierarchyNode pplc = new HierarchyNode("PPLC");
		pplc.parents.add(adm4);
		nodeMap.put(pplc.type, pplc);
		HierarchyNode ppla = new HierarchyNode("PPLA");
		ppla.parents.add(adm4);
		nodeMap.put(ppla.type, ppla);
		HierarchyNode ppla2 = new HierarchyNode("PPLA2");
		ppla2.parents.add(adm4);
		nodeMap.put(ppla2.type, ppla2);
		HierarchyNode ppla3 = new HierarchyNode("PPLA3");
		ppla3.parents.add(adm4);
		nodeMap.put(ppla3.type, ppla3);
		HierarchyNode ppla4 = new HierarchyNode("PPLA4");
		ppla4.parents.add(adm4);
		nodeMap.put(ppla4.type, ppla4);
		//assume anything else is pplx
		HierarchyNode pplx = new HierarchyNode("PPLX");
		pplx.parents.add(adm4);
		nodeMap.put(pplx.type, pplx);
	}
	
	/**
	 * @param childType - Suspected child Geoname type
	 * @param parentType - Suspected parent Geoname type
	 * @return True if childType is a child of parentType, otherwise False
	 */
	public boolean isParent(String childType, String parentType) {
		HierarchyNode child = nodeMap.get(childType);
		if (child == null) {
			child = nodeMap.get("PPLX");
		}
		HierarchyNode parent = nodeMap.get(parentType);
		if (parent == null || parent == child) {
			return false;
		}
		while (child != root) {
			if (child.parents.contains(parent)) {
				return true;
			}
			child = child.parents.iterator().next();//types are in bands, so it should not matter which parent is selected
		}
		return false;
	}
	
	/**
	 * @return the static instance of the GeoHierarchy
	 */
	public static GeoHierarchy getInstance() {
		if (hierarchy == null) {
			hierarchy = new GeoHierarchy();
		}
		return hierarchy;
	}
	
}
