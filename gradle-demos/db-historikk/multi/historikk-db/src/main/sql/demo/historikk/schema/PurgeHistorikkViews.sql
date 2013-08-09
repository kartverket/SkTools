PROMPT purging views for user @historikk_db_username@
BEGIN
  FOR i IN (SELECT object_name FROM all_objects where owner = '@historikk_db_username@' AND object_type = 'VIEW')
    LOOP
        EXECUTE IMMEDIATE('DROP VIEW "@historikk_db_username@"."' || i.object_name || '"');
    END LOOP;
END;
/

COMMIT;

PROMPT purge successfull!;
