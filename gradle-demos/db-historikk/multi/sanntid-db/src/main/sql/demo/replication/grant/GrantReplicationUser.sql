------------------------------------------------------------------------------------------------------------------------
-- Kjores som systembruker
------------------------------------------------------------------------------------------------------------------------


--REPL_ROLE:
GRANT CREATE SESSION TO "@replication_db_username@";
GRANT ALTER SESSION TO "@replication_db_username@";
GRANT SELECT ANY TABLE TO "@replication_db_username@";
GRANT SELECT ANY DICTIONARY TO "@replication_db_username@";



PROMPT Assigning grants to existing tables and views (if any)...
BEGIN
  FOR i IN (SELECT OBJECT_NAME FROM ALL_OBJECTS WHERE OWNER='@sanntid_db_username@' AND OBJECT_TYPE IN ('TABLE', 'VIEW') AND OBJECT_NAME NOT LIKE '%\_H' ESCAPE '\') LOOP
    EXECUTE IMMEDIATE('GRANT UPDATE, DELETE, INSERT, SELECT ON "@sanntid_db_schema@"."' || i.OBJECT_NAME || '" TO "@replication_db_username@"');
  END LOOP;
  COMMIT;
END;
/



