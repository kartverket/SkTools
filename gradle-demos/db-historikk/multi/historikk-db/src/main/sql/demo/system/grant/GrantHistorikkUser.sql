------------------------------------------------------------------------------------------------------------------------
-- Kjores som systembruker
------------------------------------------------------------------------------------------------------------------------
ALTER USER "@historikk_db_username@" QUOTA UNLIMITED ON "@historikk_db_tablespace_index@";
ALTER USER "@historikk_db_username@" QUOTA UNLIMITED ON "@historikk_db_tablespace@";

GRANT CONNECT TO "@historikk_db_username@";    -- CREATE SESSION

-- Brukes kun ifm explain plan. Trengs antageligvis ikke i produksjon
GRANT SELECT_CATALOG_ROLE TO "@historikk_db_username@";


PROMPT Revoking superfluous roles for user @historikk_db_username@
BEGIN
  FOR i IN (SELECT * FROM dba_role_privs WHERE GRANTEE = '@historikk_db_username@' AND GRANTED_ROLE IN (''))
    LOOP
        EXECUTE IMMEDIATE('REVOKE ' || i.GRANTED_ROLE || ' FROM "@historikk_db_username@"');
    END LOOP;
END;
/

PROMPT Revoking superfluous SYSTEM PRIVILEGIES for user @historikk_db_username@
BEGIN
  FOR i IN (SELECT * FROM SYS.DBA_SYS_PRIVS WHERE GRANTEE = '@historikk_db_username@' AND PRIVILEGE IN (''))
    LOOP
        EXECUTE IMMEDIATE('REVOKE ' || i.PRIVILEGE || ' FROM "@historikk_db_username@"');
    END LOOP;
END;
/


