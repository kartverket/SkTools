------------------------------------------------------------------------------------------------------------------------
-- Kjores som systembruker
------------------------------------------------------------------------------------------------------------------------

PROMPT Granting historikk access for @sanntid_db_username@...

GRANT EXECUTE ON "@historikk_db_schema@".SNAPSHOT_TIME TO "@sanntid_db_username@";
