package edu.asu.zoophy.pipeline;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Responsible for providing properties to non-singleton pipeline classes that cannot use Spring annotations
 * @author devdemetri
 */
public class PropertyProvider {
	
	private static Logger log;
	private static Map<String, String> properties;
	private static PropertyProvider provider = null;
	
	private PropertyProvider() throws PipelineException {
		log = Logger.getLogger("PropertyProvider");
		properties = new HashMap<String, String>();
		String propertiesPath = System.getProperty("user.dir") + "/config/application.properties";
		File propertiesFile = new File(propertiesPath);
		Scanner scan = null;
		try {
			scan = new Scanner(propertiesFile);
			while (scan.hasNextLine()) {
				String line = scan.nextLine().trim();
				if (!(line.isEmpty() || line.startsWith("#"))) {
					String[] splits = line.split("=");
					properties.put(splits[0], splits[1]);
				}
			}
			scan.close();
		} 
		catch (FileNotFoundException e) {
			log.log(Level.SEVERE, "Could not find properties file: "+propertiesPath);
			throw new PipelineException("Could not find properties file: "+propertiesPath, null);
		}
		finally {
			if (scan != null) {
				scan.close();
			}
		}
	}
	
	/**
	 * Retrieve the singleton instance of the PropertyProvider
	 * @return a PropertyProvider instance
	 * @throws PipelineException
	 */
	public static PropertyProvider getInstance() throws PipelineException {
		if (provider == null) {
			provider = new PropertyProvider();
		}
		return provider;
	}
	
	/**
	 * Retrieve the specified property's value from application.properties
	 * @param propertyName
	 * @return the specified property's value
	 */
	public String getProperty(String propertyName) {
		return properties.get(propertyName);
	}

}
