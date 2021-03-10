package util;

/**
 * This class contains SQL DDL statements used by TAGOPSIN for creating domains and relations 
 * in the local database "tagopsin" in PostgreSQL.
 *  
 * @author 	Eshan Bundhoo, University of Mauritius
 * @since	2020-01-31
 * @version	1.0
 *
 */

public class SQL_DDL1_0 {
	
	public static String getSQL() {
		
		String sql = "";
		
		//Create domains
		sql += "CREATE DOMAIN taxa_identity AS integer;\n";
		sql += "CREATE DOMAIN uniprot_accession AS varchar(15);\n";
		sql += "CREATE DOMAIN go_identity AS char(10)\n";
		sql += "    CHECK (VALUE LIKE 'GO:%');\n";
		sql += "CREATE DOMAIN pfam_accession AS char(7)\n";
		sql += "    CHECK (VALUE LIKE 'PF%');\n";
		sql += "CREATE DOMAIN pdb_identity AS char(4);\n";		
		
		//Create tables
		sql += "CREATE TABLE organism(\n";
		sql += "    oid serial NOT NULL,\n";
		sql += "    species varchar NOT NULL,\n";
		sql += "    taxonomy_id taxa_identity,\n";
		sql += "    PRIMARY KEY (oid)\n";
		sql += ");\n";
		sql += "CREATE TABLE genome(\n";
		sql += "    ac varchar(20) NOT NULL,\n";
		sql += "    nt_sequence varchar,\n";
		sql += "    oid integer NOT NULL,\n";
		sql += "    PRIMARY KEY (ac),\n";
		sql += "    FOREIGN KEY (oid) REFERENCES organism\n";
		sql += ");\n";
		sql += "CREATE TABLE cds(\n";
		sql += "    cdsid serial NOT NULL,\n";
		sql += "    gene varchar(25),\n";
		sql += "    locus_tag varchar(25),\n";
		sql += "    type varchar(15),\n";
		sql += "    product text,\n";
		sql += "    protein_id varchar(20),\n";
		sql += "    uniprot_ac uniprot_accession,\n";
		sql += "    prot_aa_seq varchar,\n";
		sql += "    genome_ac varchar(20),\n";
		sql += "    PRIMARY KEY (cdsid),\n";
		sql += "    FOREIGN KEY (genome_ac) REFERENCES genome (ac)\n";
		sql += ");\n";
		sql += "CREATE TABLE cds_ntseq(\n";
		sql += "    ntsid serial NOT NULL,\n";
		sql += "    start integer NOT NULL,\n";
		sql += "    stop integer NOT NULL,\n";
		sql += "    seq varchar,\n";
		sql += "    cdsid integer,\n";
		sql += "    PRIMARY KEY (ntsid),\n";
		sql += "    FOREIGN KEY (cdsid) REFERENCES cds\n";
		sql += ");\n";
		sql += "CREATE TABLE protein(\n";
		sql += "    uniprot_ac uniprot_accession NOT NULL,\n";
		sql += "    name text,\n";
		sql += "    function text,\n";
		sql += "    sc_location text,\n";
		sql += "    aa_sequence varchar,\n";
		sql += "    aa_seq_length smallint,\n";
		sql += "    uniprot_id varchar,\n";
		sql += "    PRIMARY KEY (uniprot_ac)\n";
		sql += ");\n";
		sql += "CREATE TABLE protein2go(\n";
		sql += "    uniprot_ac uniprot_accession NOT NULL,\n";
		sql += "    go_id go_identity NOT NULL,\n";
		sql += "    PRIMARY KEY (uniprot_ac, go_id),\n";
		sql += "    FOREIGN KEY (uniprot_ac) REFERENCES protein\n";
		sql += ");\n";
		sql += "CREATE TABLE go(\n";
		sql += "    go_id go_identity NOT NULL,\n";
		sql += "    name text NOT NULL,\n";
		sql += "    definition text,\n";
		sql += "    namespace varchar(30),\n";
		sql += "    PRIMARY KEY (go_id)\n";
		sql += ");\n";
		sql += "CREATE TABLE go_parent(\n";
		sql += "    go_id go_identity NOT NULL,\n";
		sql += "    go_id_parent go_identity NOT NULL,\n";
		sql += "    PRIMARY KEY (go_id, go_id_parent)\n";
		sql += ");\n";
		sql += "CREATE TABLE protein2pfam(\n";
		sql += "    uniprot_ac uniprot_accession NOT NULL,\n";
		sql += "    seq_start smallint NOT NULL,\n";
		sql += "    seq_stop smallint NOT NULL,\n";
		sql += "    pfam_ac pfam_accession NOT NULL,\n";
		sql += "    PRIMARY KEY (uniprot_ac, seq_start, seq_stop),\n";
		sql += "    FOREIGN KEY (uniprot_ac) REFERENCES protein\n";
		sql += ");\n";
		sql += "CREATE TABLE pfam(\n";
		sql += "    pfam_ac pfam_accession NOT NULL,\n";
		sql += "    pfam_id varchar(30) NOT NULL,\n";
		sql += "    description text,\n";
		sql += "    PRIMARY KEY (pfam_ac)\n";
		sql += ");\n";
		sql += "CREATE TABLE protein2pdb(\n";
		sql += "	uniprot_ac uniprot_accession NOT NULL,\n";
		sql += "    sp_start smallint NOT NULL,\n";
		sql += "    sp_stop smallint NOT NULL,\n";
		sql += "    pdb_id pdb_identity NOT NULL,\n";
		sql += "    chain varchar(5) NOT NULL,\n";
		sql += "    pdb_start varchar(5),\n";
		sql += "    pdb_stop varchar(5),\n";
		sql += "    --PRIMARY KEY (uniprot_ac, sp_start, pdb_id, chain, pdb_start),\n";
		sql += "    FOREIGN KEY (uniprot_ac) REFERENCES protein\n";
		sql += ");\n";
		sql += "CREATE TABLE pdb_chain(\n";
		sql += "    pdb_id pdb_identity NOT NULL,\n";
		sql += "    chain varchar(5) NOT NULL,\n";
		sql += "    aa_sequence varchar,\n";
		sql += "    taxonomy_id taxa_identity,\n";
		sql += "    PRIMARY KEY (pdb_id, chain)\n";
		sql += ");\n";
		sql += "CREATE TABLE pdb(\n";
		sql += "    pdb_id pdb_identity NOT NULL,\n";
		sql += "    name text,\n";
		sql += "    method text,\n";
		sql += "    resolution varchar(15),\n";
		sql += "    PRIMARY KEY (pdb_id)\n";
		sql += ");";
		
		return sql;
	}

}
