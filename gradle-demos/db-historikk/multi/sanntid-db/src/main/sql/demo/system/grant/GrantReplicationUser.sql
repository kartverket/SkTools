------------------------------------------------------------------------------------------------------------------------
-- Kjores som systembruker
------------------------------------------------------------------------------------------------------------------------
ALTER USER "@replication_db_username@" QUOTA UNLIMITED ON "@sanntid_db_tablespace@";
ALTER USER "@replication_db_username@" QUOTA UNLIMITED ON "@replication_db_tablespace@";


-- REVOKING SUPERFLUOUS ROLES AND PRIVILEGES...

PROMPT Revoking superfluous roles for user @replication_db_username@
BEGIN
  FOR i IN (SELECT * FROM dba_role_privs WHERE GRANTEE = '@replication_db_username@' AND GRANTED_ROLE IN
    ( --alle tidligere roller som ikke lenger er aktuelle
      ''
    ))
    LOOP
      EXECUTE IMMEDIATE('REVOKE ' || i.GRANTED_ROLE || ' FROM "@replication_db_username@"');
    END LOOP;
END;
/

PROMPT Revoking superfluous SYSTEM PRIVILEGIES for user @replication_db_username@
BEGIN
  FOR i IN (SELECT * FROM SYS.DBA_SYS_PRIVS WHERE GRANTEE = '@replication_db_username@' AND PRIVILEGE IN
    (  --alle tidligere grants som ikke lengre er aktuelle
      ''
    ))
    LOOP
      EXECUTE IMMEDIATE('REVOKE ' || i.PRIVILEGE || ' FROM "@replication_db_username@"');
    END LOOP;
END;
/

-- GRANTS...

--REPL_ROLE:
GRANT CREATE SESSION TO "@replication_db_username@";
GRANT ALTER SESSION TO "@replication_db_username@";
GRANT SELECT ANY TABLE TO "@replication_db_username@";
GRANT SELECT ANY DICTIONARY TO "@replication_db_username@";



