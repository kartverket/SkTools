------------------------------------------------------------------------------------------------------------------------
-- Kjores som admin-bruker
------------------------------------------------------------------------------------------------------------------------

PROMPT Granting historikk access for @replication_db_username@...

-- GRANT EXECUTE ON "@historikk_db_schema@".SNAPSHOT_TIME TO "@replication_db_username@";
GRANT EXECUTE ON "@historikk_db_schema@".HISTORIKK_TRANSACTION TO "@replication_db_username@";
