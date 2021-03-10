- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
TAGOPSIN -- Taxonomy, Gene, Ontology, Protein, Structure INtegrated
version 1.3, 10 March 2021
- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -


Introduction
- - - - - - - - - - - - - - -
This program retrieves select data from NCBI Taxonomy, NCBI Nucleotide, UniProtKB, 
Gene Ontology, Pfam, EBI SIFTS and RCSB PDB, and assembles them in the database 
management system PostgreSQL. TAGOPSIN is an organism-centred data warehousing 
tool that works with prokaryotic and eukaryotic organisms as well as viruses. In 
the current version, the program includes sequence data for protein isoforms.

The Pfam data file and UniProt ID mapping data file are very large. Hence, for
rapidity and efficiency, it is recommended to run the program on a high
performance computer with good Internet connection. 


Availability
- - - - - - - - - - - - - - -
https://github.com/ebundhoo/TAGOPSIN


Support
- - - - - - - - - - - - - - -
Unix-based operating systems (e.g. Linux, macOS)


Prerequisites
- - - - - - - - - - - - - - -
1.  Java 8 or higher
2.  GNU Wget
3.  PostgreSQL 9.6.5 or higher	 
4.  Basic knowledge of SQL


Pre-installation
- - - - - - - - - - - - - - -
1.  TAGOPSIN runs the PostgreSQL relational database management system. To install 
    PostgreSQL locally, type out the command <sudo apt-get -y install postgresql> 
    (for Linux Ubuntu). For additional information on installing PostgreSQL (e.g. 
    on other operating systems), please visit the web page at 
    <https://www.postgresql.org/download/>.
    
2.  A Java Database Connectivity (JDBC) driver is required to interface with 
    PostgreSQL. For example, the JDBC driver 42.2.16 used by the program is 
    compatible with Java 11.0.8.


Usage
- - - - - - - - - - - - - - -
java -jar tagopsin.jar


User input
- - - - - - - - - - - - - - -
1.  The URL location of the PostgreSQL JDBC driver is required. For example 
    <jdbc:postgresql://localhost:port_number/db_name> where "port_number" and 
    "db_name" are the port number of the server and the target database 
    respectively. Default values are "5432" and "tagopsin".

2.  The scientific name of the organism (at the genus or species level) should be 
    provided and it is case insensitive. You also need to indicate whether the 
    organism is eukaryotic (enter "E" or "e"), prokaryotic (enter "P" or "p") or 
    viral (enter "V" or "v").


Known issues
- - - - - - - - - - - - - - -
1.  Depending on your Internet bandwidth, the program Wget may not retrieve all 
    genomic data files from NCBI Nucleotide. As a workaround, TAGOPSIN iteratively 
    tries to download any missing data file until the correct number of files has 
    been downloaded.

2.  A memory error relating to the Java heap space can arise for very large 
    datasets. In this case, drop the database "tagopsin" in pgAdmin and re-run 
    the program using the command "java -Xms1G -Xmx2G -jar tagopsin.jar", where 
    -Xms and -Xmx set the initial and maximum Java heap sizes respectively (1 GB 
    and 2 GB in this example).

3.  If you need to restart the program due to premature termination, it is 
    important to first drop the database "tagopsin" in pgAdmin.


Authors and contact details
- - - - - - - - - - - - - - -
Eshan Bundhoo, Anisah W. Ghoorah and Yasmina Jaufeerally-Fakim
University of Mauritius
Reduit 80837
Mauritius.
mohammad.bundhoo6@umail.uom.ac.mu
a.ghoorah@uom.ac.mu


License
- - - - - - - - - - - - - - -
Licensed under the GNU General Public License

