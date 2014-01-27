PROMPT purging packages for user @historikk_index1_db_username@
BEGIN
  FOR i IN (SELECT object_name FROM all_objects where owner = '@historikk_index1_db_username@' AND object_type = 'PACKAGE')
    LOOP
        EXECUTE IMMEDIATE('DROP PACKAGE "@historikk_index1_db_username@"."' || i.object_name || '"');
    END LOOP;
END;
/


PROMPT purge successfull!;
