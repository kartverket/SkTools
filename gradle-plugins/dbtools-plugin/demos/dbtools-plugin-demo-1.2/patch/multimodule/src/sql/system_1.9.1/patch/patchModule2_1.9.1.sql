-- Patcher for Module2 til versjon 1.9.1

-- PATCH DB.MIN.VERSION="<any>"

-- PATCH DATA DB.VERSION="1.9.1" PATCH.NO="1" "Create test table2"

CREATE TABLE TEST_TABLE2 (
   ID INTEGER NOT NULL,
   NAVN VARCHAR(32) NOT NULL,
   PRIMARY KEY (ID)
);

