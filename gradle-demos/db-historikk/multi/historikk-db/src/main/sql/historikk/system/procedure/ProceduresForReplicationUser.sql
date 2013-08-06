------------------------------------------------------------------------------------------------------------------------
-- Kjores som systembruker
------------------------------------------------------------------------------------------------------------------------

PROMPT Adding replication procedures for @replication_db_username@...

CREATE OR REPLACE PROCEDURE "@replication_db_schema@".Set_T_Trans(timestampAsString IN VARCHAR2) AUTHID DEFINER
IS
BEGIN
  "@historikk_db_schema@".HISTORIKK_TRANSACTION.Set_T_Trans( "@historikk_db_schema@".HISTORIKK_TRANSACTION.To_T(timestampAsString), 2+4 );
END Set_T_Trans;
/


-- Golden gate konfigurasjon for setting av transaksjons-tidspunkt for traildata

-- TABLE REGINA.* , SQLEXEC ( &
--  SPNAME Set_T_Trans , &
--  PARAMS ( REQUIRED timestampAsString = @GETENV ( "GGHEADER" , "COMMITTIMESTAMP" ) ) , &
--  BEFOREFILTER &
-- );