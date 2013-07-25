------------------------------------------------------------------------------------------------------------------------
-- Kjores som systembruker
-- Benytter forenklet BIGFILE da denne kan opprette en enkel fil som kan bli større enn 32GB (limit ved 8k blokker)
-- For lokal utvikling hvor man ikke ønsker backup er dette et mer optimalt oppsett.
------------------------------------------------------------------------------------------------------------------------

CREATE BIGFILE TABLESPACE "@sanntid_db_tablespace@"
  DATAFILE
    '@db_oradata01@\@sanntid_db_sid@\@sanntid_db_tablespace@.DBF' SIZE 32M REUSE AUTOEXTEND ON NEXT 32M MAXSIZE UNLIMITED
  LOGGING
  EXTENT MANAGEMENT LOCAL
  SEGMENT SPACE MANAGEMENT AUTO
;