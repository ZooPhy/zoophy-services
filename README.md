# zoophy-services
RESTful services for ZooPhy. This consists of:

1) Retreiving GenBankRecords from the ZooPhy SQL Database

2) Searching GenBankRecords in the ZooPhy Lucene Index

3) Running ZooPhy Pipeline jobs

## Requirements:
* [JDK 1.7.x] (http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html)
* [Maven 3.x] (https://maven.apache.org/download.cgi)
* [PostgreSQL 9.x] (https://www.postgresql.org/download/) for SQL Database
* [Lucene 3.x] (http://archive.apache.org/dist/lucene/java/3.0.3/) for Lucene Index
* Java IDE, [Spring Tool Suite] (https://spring.io/tools) is heavily recommended for best Spring integration

## Setup:

1) Import the project into an IDE as "Existing Maven Project"

2) Create an application.properties file in the config folder with your SQL and Lucene details. Refer to [application.properties.template](config/application.properties.template)

3) Run the project as "Maven Build". Set the Maven build goals to "clean package"

4) The build should run successfully and generate a runnable jar in the target folder. This can be run via terminal, or in Spring Tool Suite click Run As "Spring Boot App"

## Using Services
The current services may be used via GET requests. They return data in JSON format:

### Get GenBankRecord:
* Path: /record?accession=\<Genbank Accession>
* Example: http://zodo.asu.edu:8008/record?accession=KU296559

### Get Location of GenBankRecord
* Path: /location?accession=\<Genbank Accession>
* Example: http://zodo.asu.edu:8008/location?accession=KU296559

### Lucene query for Index records
* Path: /search?query=\<URL Encoded Lucene Query>
* Lucene Terms: TaxonID, GeonameID, HostID, Gene, SegmentLength, Date, etc.
* Example Lucene Query: TaxonID:114727 AND Gene:(HA NOT Complete) AND Date:[2007 TO 20081231] AND HostID:9606 AND GeonameID: (5551752 OR 5332921 OR 5855797 OR 5509151) AND SegmentLength:[1650 TO 9999] 
* Example Request:  http://zodo.asu.edu:8008/search?query=TaxonID%3A114727%20AND%20Gene%3A(HA%20NOT%20Complete)%20AND%20Date%3A%5B2007%20TO%2020081231%5D%20AND%20HostID%3A9606%20AND%20GeonameID%3A%20(5551752%20OR%205332921%20OR%205855797%20OR%205509151)%20AND%20SegmentLength%3A%5B1650%20TO%209999%5D 
* Details on Lucene Query syntax can be found [here](https://lucene.apache.org/core/2_9_4/queryparsersyntax.html).
* Note: Index records DO NOT contain all of the record details, just the common details used by our Lucene powered Search Engine. 

