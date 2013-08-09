PROMPT purging tables for user @historikk_db_username@
BEGIN
  FOR i IN (SELECT table_name FROM all_tables where owner = '@historikk_db_username@' and table_name not like '%$%')
    LOOP
        EXECUTE IMMEDIATE('DROP TABLE "@historikk_db_username@"."' || i.table_name || '" CASCADE CONSTRAINTS PURGE');
    END LOOP;
END;
/

PROMPT purge successfull!;
