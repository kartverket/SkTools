PROMPT purging packages for user @historikk_db_username@
BEGIN
  FOR i IN (SELECT object_name FROM all_objects where owner = '@historikk_db_username@' AND object_type = 'PACKAGE')
    LOOP
        EXECUTE IMMEDIATE('DROP PACKAGE @historikk_db_username@.' || i.object_name);
    END LOOP;
END;
/


PROMPT purge successfull!;
