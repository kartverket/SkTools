------------------------------------------------------------------------------------------------------------------------
-- Kjores som systembruker
------------------------------------------------------------------------------------------------------------------------

ALTER USER "@historikk_db_username@" QUOTA UNLIMITED ON "@historikk_db_tablespace@";
ALTER USER "@historikk_db_username@" QUOTA UNLIMITED ON "@historikk_db_tablespace_index@";

GRANT CREATE SESSION TO "@historikk_db_username@";
GRANT RESOURCE TO "@historikk_db_username@";
GRANT CREATE SYNONYM TO "@historikk_db_username@";
GRANT CREATE TABLE TO "@historikk_db_username@";
GRANT CREATE TRIGGER TO "@historikk_db_username@";
GRANT CREATE VIEW TO "@historikk_db_username@";

-- Historikk bruker eier og oppretter triggere for tabeller ifra sanntid skjema ...
GRANT CREATE ANY TRIGGER TO "@historikk_db_username@";

-- Brukes kun ifm explain plan. Trengs antageligvis ikke i produksjon
GRANT SELECT_CATALOG_ROLE TO "@historikk_db_username@";