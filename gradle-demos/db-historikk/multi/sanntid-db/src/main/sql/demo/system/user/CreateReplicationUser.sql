------------------------------------------------------------------------------------------------------------------------
-- Kjores som systembruker
------------------------------------------------------------------------------------------------------------------------

PROMPT Creating Replication user @replication_db_username@...

CREATE USER "@replication_db_username@" IDENTIFIED BY "@replication_db_password@"
  DEFAULT TABLESPACE "@replication_db_tablespace@"
  TEMPORARY TABLESPACE "TEMP"
  ACCOUNT UNLOCK
;

ALTER USER "@replication_db_username@" QUOTA UNLIMITED ON "@sanntid_db_tablespace@";
ALTER USER "@replication_db_username@" QUOTA UNLIMITED ON "@replication_db_tablespace@";

--REPL_ROLE:
GRANT CREATE SESSION TO "@replication_db_username@";
GRANT ALTER SESSION TO "@replication_db_username@";
GRANT SELECT ANY TABLE TO "@replication_db_username@";
GRANT SELECT ANY DICTIONARY TO "@replication_db_username@";
