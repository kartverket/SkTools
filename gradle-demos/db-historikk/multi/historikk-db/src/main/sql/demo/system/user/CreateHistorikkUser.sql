------------------------------------------------------------------------------------------------------------------------
-- Kjores som systembruker
------------------------------------------------------------------------------------------------------------------------

PROMPT Creating database user @historikk_db_username@...

CREATE USER "@historikk_db_username@" IDENTIFIED BY "@historikk_db_password@"
  DEFAULT TABLESPACE "@historikk_db_tablespace@"
  TEMPORARY TABLESPACE "TEMP"
  ACCOUNT UNLOCK
;

ALTER USER "@historikk_db_username@" QUOTA UNLIMITED ON "@historikk_db_tablespace_index@";
ALTER USER "@historikk_db_username@" QUOTA UNLIMITED ON "@historikk_db_tablespace@";

GRANT CREATE SESSION TO "@historikk_db_username@";
GRANT RESOURCE TO "@historikk_db_username@";
