PROMPT purging synonyms for user @sanntid_db_username@
BEGIN
  FOR i IN (SELECT object_name FROM all_objects where owner = '@sanntid_db_username@' AND object_type = 'SYNONYM')
    LOOP
        EXECUTE IMMEDIATE('DROP SYNONYM @sanntid_db_username@.' || i.object_name);
    END LOOP;
END;
/


PROMPT purge successfull!;
