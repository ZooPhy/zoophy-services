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
