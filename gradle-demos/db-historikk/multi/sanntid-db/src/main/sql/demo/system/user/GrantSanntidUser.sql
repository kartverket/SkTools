------------------------------------------------------------------------------------------------------------------------
-- Kjores som systembruker
------------------------------------------------------------------------------------------------------------------------

ALTER USER "@sanntid_db_username@" QUOTA UNLIMITED ON "@sanntid_db_tablespace@";
ALTER USER "@sanntid_db_username@" QUOTA UNLIMITED ON "@sanntid_db_tablespace_index@";

GRANT CREATE SESSION TO "@sanntid_db_username@";
GRANT RESOURCE TO "@sanntid_db_username@";
GRANT CREATE SYNONYM TO "@sanntid_db_username@";
GRANT CREATE TABLE TO "@sanntid_db_username@";
GRANT CREATE TRIGGER TO "@sanntid_db_username@";
--GRANT CREATE VIEW TO "@sanntid_db_username@";

-- Brukes kun ifm explain plan. Trengs antageligvis ikke i produksjon
GRANT SELECT_CATALOG_ROLE TO "@sanntid_db_username@";
