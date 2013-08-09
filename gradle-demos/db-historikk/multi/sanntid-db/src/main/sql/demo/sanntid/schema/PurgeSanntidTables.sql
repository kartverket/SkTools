PROMPT purging tables for user @sanntid_db_username@
BEGIN
  FOR i IN (SELECT table_name FROM all_tables where owner = '@sanntid_db_username@' and table_name not like '%$%')
    LOOP
        EXECUTE IMMEDIATE('DROP TABLE "@sanntid_db_username@"."' || i.table_name || '" CASCADE CONSTRAINTS PURGE');
    END LOOP;
END;
/


PROMPT purge successfull!;
