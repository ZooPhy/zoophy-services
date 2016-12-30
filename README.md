# zoophy-services
RESTful services for [ZooPhy] (http://zodo.asu.edu:7070/zoophy/). This consists of:

1) Retrieving GenBankRecords from the ZooPhy SQL Database

2) Searching GenBankRecords in the ZooPhy Lucene Index

3) Starting/Stopping ZooPhy Pipeline jobs

## Requirements:
* [JDK 1.7.x] (http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html)
* [Maven 3.x] (https://maven.apache.org/download.cgi)
* [PostgreSQL 9.x] (https://www.postgresql.org/download/) for SQL Database
* [Lucene 3.x] (http://archive.apache.org/dist/lucene/java/3.0.3/) for Lucene Index
* Java IDE, [Spring Tool Suite] (https://spring.io/tools) is heavily recommended for best Spring integration
* [MAFFT 7.x] (http://mafft.cbrc.jp/alignment/software/)
* [BeastGen 1.0.2] (http://beast.bio.ed.ac.uk/beastgen)
* [BEAST 1.8.4] (https://github.com/beast-dev/beast-mcmc/releases/tag/v1.8.4)
* [SpreaD3 0.9.6] (https://rega.kuleuven.be/cev/ecv/software/SpreaD3)

## Setup:

1) Import the project into an IDE as "Existing Maven Project"

2) Create an application.properties file in the config folder with your SQL and Lucene details. Refer to [application.properties.template](config/application.properties.template)

3) Run the project as "Maven Build". Set the Maven build goals to "clean package"

4) The build should run successfully and generate a runnable jar in the target folder. This can be run via terminal, or in Spring Tool Suite click Run As "Spring Boot App"

## Using Services
The current services may be used via GET requests. They return data in JSON format:

### Get GenBankRecord:
* Type: GET
* Path: /record?accession=\<Genbank Accession>
* Example: http://zodo.asu.edu:7007/record?accession=KU296559

### Get Location of GenBankRecord
* Type: GET
* Path: /location?accession=\<Genbank Accession>
* Example: http://zodo.asu.edu:7007/location?accession=KU296559

### Lucene query for Index records
* Type: GET
* Path: /search?query=\<URL Encoded Lucene Query>
* Lucene Terms: TaxonID, GeonameID, HostID, Gene, SegmentLength, Date, etc.
* Example Lucene Query: TaxonID:114727 AND Gene:(HA NOT Complete) AND Date:[2007 TO 20081231] AND HostID:9606 AND GeonameID: (5551752 OR 5332921 OR 5855797 OR 5509151) AND SegmentLength:[1650 TO 9999] 
* Example Request:  http://zodo.asu.edu:7007/search?query=TaxonID%3A114727%20AND%20Gene%3A(HA%20NOT%20Complete)%20AND%20Date%3A%5B2007%20TO%2020081231%5D%20AND%20HostID%3A9606%20AND%20GeonameID%3A%20(5551752%20OR%205332921%20OR%205855797%20OR%205509151)%20AND%20SegmentLength%3A%5B1650%20TO%209999%5D 
* Details on Lucene Query syntax can be found [here](https://lucene.apache.org/core/2_9_4/queryparsersyntax.html).
* Note: Index records DO NOT contain all of the record details, just the common details used by our Lucene powered Search Engine. 

### Lucene query for specific list of Accessions
* Type: POST
* Path: /search/accession
* Required POST Body Data: JSON list of valid accession Strings
* Example POST body: 
```
['GQ258462','CY055940','CY055932','CY055788','CY055780','CY055740','CY055661','HQ712184','HM624085']
```

* Note: This service is a work around for searching long specific lists of accessions, rather than using absurdly long GET request URLs. Unfortunately, the current limit is 1000 accessions. This will be refactored in a future PR.* 

### Start ZooPhy Job
* Type: POST
* Path: /run
* Required POST Body Data: JSON Object containing:
 * replyEmail - String
 * jobName - String (optional)
 * accessions - List of Strings (Limit 1000)
* Example POST Body:
```
{
  "replyEmail": 'fake@gmail.com',
  "jobName": 'Australia H1N1 Human HA 09',
  "accessions": ['GQ258462','CY055940','CY055932','CY055788','CY055780','CY055740','CY055661','HQ712184','HM624085']
}
```

* Note: The ZooPhy Pipeline ties together several packages of complex software that may fail for numerous reasons. A common reason is having too few or too many unique disjoint Geoname locations (must have between 2 and 50). Jobs may also take very long to run, and time estimates will be provided in update emails. 

### Stop ZooPhy Job
* Type: GET
* Path: /stop?id=\<Zoophy Job ID>
* Example Request: http://zodo.asu.edu:7007/stop?id=cbb6b262-44c6-4291-8262-76da11b4f07d 
* Note: Security/Authentication to prevent malicously killing jobs will be added in a future PR. For now we are relying on the randomy generated UUID's.
