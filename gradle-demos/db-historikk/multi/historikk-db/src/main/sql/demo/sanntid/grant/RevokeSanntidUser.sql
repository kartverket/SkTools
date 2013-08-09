------------------------------------------------------------------------------------------------------------------------
-- Kjores som systembruker
------------------------------------------------------------------------------------------------------------------------

PROMPT Revoking historikk access for @sanntid_db_username@...

REVOKE ALL ON "@historikk_db_schema@".SNAPSHOT_TIME FROM "@sanntid_db_username@";
REVOKE ALL ON "@historikk_db_schema@".HISTORIKK_TRANSACTION FROM "@sanntid_db_username@";
