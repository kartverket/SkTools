-- AUTHID DEFINER fører til at funksjonene blir kjørt med rettigheter til historikk-db-bruker (brukeren som oppretter/eier denne)
CREATE OR REPLACE PACKAGE "@historikk_index_db_schema@".SNAPSHOT_TIME AUTHID DEFINER AS

    FUNCTION Get_T_CURRENT RETURN TIMESTAMP;

    -- Finner gjeldende timestamp (default er T_CURRENT )
    FUNCTION Get_T RETURN SNAPSHOT_TRANS.v%TYPE;
    -- Setter gjeldende timestamp
    FUNCTION Set_T(newValue IN SNAPSHOT_TRANS.v%TYPE) RETURN SNAPSHOT_TRANS.v%TYPE;


    -- Konverterer timestamp ifra fast streng-representasjon
    FUNCTION To_T(timestampAsString IN VARCHAR2) RETURN TIMESTAMP;

END SNAPSHOT_TIME;
/

CREATE OR REPLACE PACKAGE BODY "@historikk_index_db_schema@".SNAPSHOT_TIME AS

    -- Betegner timestamp for gjeldende snapshot versjon av objektet. Kan betegnes som den versjonen som er 'levende'. Ref tEnd kolonne.
    t_Current CONSTANT SNAPSHOT_TRANS.v%TYPE := SNAPSHOT_TIME.To_T('9999-01-01 00:00:00.00');

    -- initialiserer t slik at man får oppdaterte data i views som default
    t SNAPSHOT_TRANS.v%TYPE := t_Current;



    FUNCTION Get_T_CURRENT RETURN TIMESTAMP IS
    BEGIN
      RETURN t_Current;
    END Get_T_CURRENT;

    FUNCTION Get_T RETURN SNAPSHOT_TRANS.v%TYPE IS
    BEGIN
      RETURN t;
    END Get_T;

    FUNCTION Set_T(newValue IN SNAPSHOT_TRANS.v%TYPE) RETURN SNAPSHOT_TRANS.v%TYPE IS
    BEGIN
      t:= newValue;
      RETURN t;
    END Set_T;

    FUNCTION To_T(timestampAsString IN VARCHAR2) RETURN TIMESTAMP IS
    BEGIN
      RETURN to_timestamp(timestampAsString, 'YYYY-MM-DD HH24:MI:SS.FF');
    END To_T;



END SNAPSHOT_TIME;
/