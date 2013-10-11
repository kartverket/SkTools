------------------------------------------------------------------------------------------------------------------------
-- Kjores som systembruker
------------------------------------------------------------------------------------------------------------------------

PROMPT Revoking historikk access for @replication_db_username@...

REVOKE ALL ON "@historikk_db_schema@".SNAPSHOT_TIME FROM "@replication_db_username@";
REVOKE ALL ON "@historikk_db_schema@".HISTORIKK_TRANSACTION FROM "@replication_db_username@";
