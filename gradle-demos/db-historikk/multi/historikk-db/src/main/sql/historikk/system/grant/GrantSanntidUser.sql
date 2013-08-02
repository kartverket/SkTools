------------------------------------------------------------------------------------------------------------------------
-- Kjores som systembruker
------------------------------------------------------------------------------------------------------------------------

PROMPT Granting historikk access for @sanntid_db_username@...

GRANT EXECUTE ON "@historikk_db_schema@".SNAPSHOT_TIME TO "@sanntid_db_username@";

-- Kan velge om sanntidsbruker skal ha tilgang til aa modifisere paa transaksjonsnivaa
--GRANT EXECUTE ON "@historikk_db_schema@".HISTORIKK_TRANSACTION TO "@sanntid_db_username@";
