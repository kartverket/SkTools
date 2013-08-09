------------------------------------------------------------------------------------------------------------------------
-- Kjores som systembruker
------------------------------------------------------------------------------------------------------------------------

PROMPT Creating Replication user @replication_db_username@...

CREATE USER "@replication_db_username@" IDENTIFIED BY "@replication_db_password@" DEFAULT TABLESPACE "@replication_db_tablespace@" TEMPORARY TABLESPACE "TEMP" ACCOUNT UNLOCK;

