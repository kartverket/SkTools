------------------------------------------------------------------------------------------------------------------------
-- Kjores som systembruker
------------------------------------------------------------------------------------------------------------------------

PROMPT Creating database user @historikk_index_db_username@...

CREATE USER "@historikk_index_db_username@" IDENTIFIED BY "@historikk_index_db_password@"
  DEFAULT TABLESPACE "@historikk_index_db_tablespace@"
  TEMPORARY TABLESPACE "TEMP"
  ACCOUNT UNLOCK
;

