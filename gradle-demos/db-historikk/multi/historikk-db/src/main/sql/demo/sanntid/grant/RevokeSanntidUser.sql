------------------------------------------------------------------------------------------------------------------------
-- Kjores som systembruker
------------------------------------------------------------------------------------------------------------------------

PROMPT Revoking historikk access for @sanntid_db_username@...

REVOKE ALL ON "@historikk_db_schema@".SNAPSHOT_TIME FROM "@sanntid_db_username@";
REVOKE ALL ON "@historikk_db_schema@".HISTORIKK_TRANSACTION FROM "@sanntid_db_username@";

BEGIN
  FOR i IN (SELECT object_name FROM all_objects where owner = '@historikk_db_username@' AND object_type IN ('VIEW', 'TABLE')) LOOP
    EXECUTE IMMEDIATE('REVOKE ALL ON "@historikk_db_schema@"."' || i.object_name || '" FROM "@sanntid_db_username@"');
  END LOOP;
  COMMIT;
END;
/
