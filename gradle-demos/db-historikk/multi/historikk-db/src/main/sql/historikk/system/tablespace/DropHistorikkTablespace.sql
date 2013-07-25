--***************************************************************************
--
-- Kjores som systembruker
--
-- Dropper tablespace samt evt objekter (data) som befinner seg der
--
-- PS: kjører CASCADE CONSTRAINTS da en forutsetter at
--     man har kontroll på hva man sletter. Dette fører til at fysiske filer faktisk blir borte.
--***************************************************************************
DROP TABLESPACE "@historikk_db_tablespace@"
  INCLUDING CONTENTS AND DATAFILES
  CASCADE CONSTRAINTS
;