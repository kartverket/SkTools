------------------------------------------------------------------------------------------------------------------------
-- Kjores som systembruker
------------------------------------------------------------------------------------------------------------------------

GRANT CREATE SESSION TO "@historikk_db_username@";
GRANT RESOURCE TO "@historikk_db_username@";


-- Brukes kun ifm explain plan. Trengs antageligvis ikke i produksjon
GRANT SELECT_CATALOG_ROLE TO "@historikk_db_username@";