
-- tom tabell etter hver transaksjon (ON COMMIT DELETE ROWS)
CREATE GLOBAL TEMPORARY TABLE "@historikk_db_schema@".SNAPSHOT_TRANS (v TIMESTAMP(9)) ON COMMIT DELETE ROWS;
