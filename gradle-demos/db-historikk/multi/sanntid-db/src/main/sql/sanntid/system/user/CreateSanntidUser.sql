------------------------------------------------------------------------------------------------------------------------
-- Kjores som systembruker
------------------------------------------------------------------------------------------------------------------------

PROMPT Creating database user @sanntid_db_username@...

CREATE USER "@sanntid_db_username@" IDENTIFIED BY "@sanntid_db_password@"
  DEFAULT TABLESPACE "@sanntid_db_tablespace@"
  TEMPORARY TABLESPACE "TEMP"
  ACCOUNT UNLOCK
;
