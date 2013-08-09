------------------------------------------------------------------------------------------------------------------------
-- Kjores som systembruker
------------------------------------------------------------------------------------------------------------------------

PROMPT Granting historikk access for @sanntid_db_username@...

GRANT EXECUTE ON "@historikk_db_schema@".SNAPSHOT_TIME TO "@sanntid_db_username@";

-- Kan velge om sanntidsbruker skal ha tilgang til aa modifisere paa transaksjonsnivaa  (slått på for denne demoen)
GRANT EXECUTE ON "@historikk_db_schema@".HISTORIKK_TRANSACTION TO "@sanntid_db_username@";

-- Kan velge om sanntidsbruker skal kunne leSE historikk-data eller ikke (slått på for denne demoen)
BEGIN
  FOR i IN (SELECT object_name FROM all_objects where owner = '@historikk_db_username@' AND object_type = 'VIEW') LOOP
    EXECUTE IMMEDIATE('GRANT SELECT ON "@historikk_db_schema@"."' || i.object_name || '" TO "@sanntid_db_username@"');
  END LOOP;
  COMMIT;
END;
/
BEGIN
  FOR i IN (SELECT object_name FROM all_objects where owner = '@historikk_db_username@' AND object_type = 'TABLE') LOOP
    EXECUTE IMMEDIATE('GRANT SELECT ON "@historikk_db_schema@"."' || i.object_name || '" TO "@sanntid_db_username@"');
  END LOOP;
  COMMIT;
END;
/