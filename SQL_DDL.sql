CREATE DOMAIN taxa_identity AS integer;
CREATE DOMAIN uniprot_accession AS varchar(15);
CREATE DOMAIN go_identity AS char(10)
    CHECK (VALUE LIKE 'GO:%');
CREATE DOMAIN pfam_accession AS char(7)
    CHECK (VALUE LIKE 'PF%');
CREATE DOMAIN pdb_identity AS char(4);
CREATE TABLE organism(
    oid serial NOT NULL,
    species varchar NOT NULL,
    taxonomy_id taxa_identity,
    PRIMARY KEY (oid)
);
CREATE TABLE genome(
    ac varchar(20) NOT NULL,
    nt_sequence varchar,
    oid integer NOT NULL,
    PRIMARY KEY (ac),
    FOREIGN KEY (oid) REFERENCES organism
);
CREATE TABLE cds(
    cdsid serial NOT NULL,
    gene varchar(25),
    locus_tag varchar(25),
    type varchar(15),
    product text,
    protein_id varchar(20),
    uniprot_ac uniprot_accession,
    prot_aa_seq varchar,
    genome_ac varchar(20),
    PRIMARY KEY (cdsid),
    FOREIGN KEY (genome_ac) REFERENCES genome (ac)
);
CREATE TABLE cds_ntseq(
    ntsid serial NOT NULL,
    start integer NOT NULL,
    stop integer NOT NULL,
    seq varchar,
    cdsid integer,
    PRIMARY KEY (ntsid),
    FOREIGN KEY (cdsid) REFERENCES cds
);
CREATE TABLE protein(
    uniprot_ac uniprot_accession NOT NULL,
    name text,
    function text,
    sc_location text,
    aa_sequence varchar,
    aa_seq_length smallint,
    uniprot_id varchar,
    PRIMARY KEY (uniprot_ac)
);
CREATE TABLE protein2go(
    uniprot_ac uniprot_accession NOT NULL,
    go_id go_identity NOT NULL,
    PRIMARY KEY (uniprot_ac, go_id),
    FOREIGN KEY (uniprot_ac) REFERENCES protein
);
CREATE TABLE go(
    go_id go_identity NOT NULL,
    name text NOT NULL,
    definition text,
    namespace varchar(30),
    PRIMARY KEY (go_id)
);
CREATE TABLE go_parent(
    go_id go_identity NOT NULL,
    go_id_parent go_identity NOT NULL,
    PRIMARY KEY (go_id, go_id_parent)
);
CREATE TABLE protein2pfam(
    uniprot_ac uniprot_accession NOT NULL,
    seq_start smallint NOT NULL,
    seq_stop smallint NOT NULL,
    pfam_ac pfam_accession NOT NULL,
    PRIMARY KEY (uniprot_ac, seq_start, seq_stop),
    FOREIGN KEY (uniprot_ac) REFERENCES protein
);
CREATE TABLE pfam(
    pfam_ac pfam_accession NOT NULL,
    pfam_id varchar(30) NOT NULL,
    description text,
    PRIMARY KEY (pfam_ac)
);
CREATE TABLE protein2pdb(
	uniprot_ac uniprot_accession NOT NULL,
    sp_start smallint NOT NULL,
    sp_stop smallint NOT NULL,
    pdb_id pdb_identity NOT NULL,
    chain varchar(5) NOT NULL,
    pdb_start varchar(5),
    pdb_stop varchar(5),
    --PRIMARY KEY (uniprot_ac, sp_start, pdb_id, chain, pdb_start),
    FOREIGN KEY (uniprot_ac) REFERENCES protein
);
CREATE TABLE pdb_chain(
    pdb_id pdb_identity NOT NULL,
    chain varchar(5) NOT NULL,
    aa_sequence varchar,
    taxonomy_id taxa_identity,
    PRIMARY KEY (pdb_id, chain)
);
CREATE TABLE pdb(
    pdb_id pdb_identity NOT NULL,
    name text,
    method text,
    resolution varchar(15),
    PRIMARY KEY (pdb_id)
);
/*
To enforce referential integrity after inserting data

ALTER TABLE go_parent 
    ADD CONSTRAINT go_parent_go_id_fkey FOREIGN KEY (go_id) REFERENCES go (go_id),
    ADD CONSTRAINT go_parent_go_id_parent_fkey FOREIGN KEY (go_id_parent) REFERENCES go (go_id);
ALTER TABLE protein2go
    ADD CONSTRAINT protein2go_go_id_fkey FOREIGN KEY (go_id) REFERENCES go (go_id);
ALTER TABLE protein2pfam
	ADD CONSTRAINT protein2pfam_pfam_ac_fkey FOREIGN KEY (pfam_ac) REFERENCES pfam (pfam_ac);
ALTER TABLE protein2pdb
    ADD CONSTRAINT protein2pdb_pdb_id_chain_fkey FOREIGN KEY (pdb_id, chain) REFERENCES pdb_chain (pdb_id, chain);
ALTER TABLE pdb_chain
	ADD CONSTRAINT pdb_chain_pdb_id_fkey FOREIGN KEY (pdb_id) REFERENCES pdb (pdb_id);
*/

/*
The following foreign key constraint cannot be enforced because the CDS uniprot_ac column contains entries from both
SwissProt and TrEMBL while the referenced column uniprot_ac in Protein relation contains only SwissProt entries.

ALTER TABLE cds 
    ADD CONSTRAINT cds_uniprot_ac_fkey FOREIGN KEY (uniprot_ac) REFERENCES protein (uniprot_ac);
*/