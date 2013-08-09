CREATE OR REPLACE SYNONYM "@sanntid_db_schema@".SNAPSHOT_TIME FOR "@historikk_db_schema@".SNAPSHOT_TIME;

-- må vurdere om sanntidsbruker skal trenge å være i stand til å sette transaksjonstidspunkt eller ikke... trolig ikke. (slått på for denne demoen)
CREATE OR REPLACE SYNONYM "@sanntid_db_schema@".HISTORIKK_TRANSACTION FOR "@historikk_db_schema@".HISTORIKK_TRANSACTION;

-- velger at bruker for sanntids-db kan se historikktabell (slått på for denne demoen)
BEGIN
  FOR i IN (SELECT object_name FROM all_objects where owner = '@historikk_db_username@' AND object_type = 'TABLE' AND object_name like '%\_H' escape '\') LOOP
    EXECUTE IMMEDIATE('CREATE OR REPLACE SYNONYM "@sanntid_db_schema@"."' || i.object_name || '" FOR "@historikk_db_schema@"."' || i.object_name || '"');
  END LOOP;
  COMMIT;
END;
/
