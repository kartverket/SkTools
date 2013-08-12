------------------------------------------------------------------------------------------------------------------------
-- Kjores som systembruker
------------------------------------------------------------------------------------------------------------------------
ALTER USER "@sanntid_db_username@" QUOTA UNLIMITED ON "@sanntid_db_tablespace_index@";
ALTER USER "@sanntid_db_username@" QUOTA UNLIMITED ON "@sanntid_db_tablespace@";

GRANT CONNECT TO "@sanntid_db_username@"; -- CREATE SESSION

-- Brukes kun ifm explain plan. Trengs antageligvis ikke i produksjon
GRANT SELECT_CATALOG_ROLE TO "@sanntid_db_username@";


PROMPT Revoking superfluous roles for user @sanntid_db_username@
BEGIN
  FOR i IN (SELECT * FROM dba_role_privs WHERE GRANTEE = '@sanntid_db_username@' AND GRANTED_ROLE IN ('RESOURCE'))
    LOOP
        EXECUTE IMMEDIATE('REVOKE ' || i.GRANTED_ROLE || ' FROM "@sanntid_db_username@"');
    END LOOP;
END;
/

PROMPT Revoking superfluous SYSTEM PRIVILEGIES for user @sanntid_db_username@
BEGIN
  FOR i IN (SELECT * FROM SYS.DBA_SYS_PRIVS WHERE GRANTEE = '@sanntid_db_username@' AND PRIVILEGE IN (''))
    LOOP
        EXECUTE IMMEDIATE('REVOKE ' || i.PRIVILEGE || ' FROM "@sanntid_db_username@"');
    END LOOP;
END;
/

