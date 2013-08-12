
-- tabell med alle historikkfelter
CREATE TABLE "@sanntid_db_schema@".STED (
    ID NUMBER(19,0) NOT NULL,
    STEDSNAVN VARCHAR2(255 CHAR) NOT NULL,
    TBEGIN TIMESTAMP(9),
    TBEGIN_INFO VARCHAR2(255 CHAR),

    CONSTRAINT P_PK_STED PRIMARY KEY (ID)
);

-- tabell med kun historikkfelt: oppdateringsdato/tbegin
CREATE TABLE "@sanntid_db_schema@".KOMMUNE (
    ID NUMBER(19,0) NOT NULL,
    KOMMUNENUMMER VARCHAR2(16 CHAR),
    KOMMUNENAVN VARCHAR2(255 CHAR),
    TBEGIN TIMESTAMP(9),

    CONSTRAINT P_PK_KOMMUNE PRIMARY KEY (ID)
);


-- koblingstabell (uten historikkfelter)
CREATE TABLE "@sanntid_db_schema@".KOMMUNERFORSTED (
    STEDID NUMBER(19,0) NOT NULL,
    KOMMUNEID NUMBER(19,0) NOT NULL,

    CONSTRAINT P_PK_KOMMUNERFORSTED PRIMARY KEY (STEDID, KOMMUNEID)
);


-- tabell uten historikk (ignoreres i kodegenerering av historikk-skjema; se task genHistorikkSchemaTempateFromSchema
CREATE TABLE "@sanntid_db_schema@".SEQUENCE (
    ID NUMBER(19,0) NOT NULL,
    VALUE NUMBER(19,0) NOT NULL,
    CONSTRAINT P_PK_SEQUENCE PRIMARY KEY (ID)
);



ALTER TABLE "@sanntid_db_schema@".KOMMUNERFORSTED
    ADD CONSTRAINT FK_KOMFORSTED_KOMMUNE FOREIGN KEY (KOMMUNEID) REFERENCES "@sanntid_db_schema@".KOMMUNE
;

ALTER TABLE "@sanntid_db_schema@".KOMMUNERFORSTED
    ADD CONSTRAINT FK_KOMFORSTED_STED FOREIGN KEY (STEDID) REFERENCES "@sanntid_db_schema@".STED
;
