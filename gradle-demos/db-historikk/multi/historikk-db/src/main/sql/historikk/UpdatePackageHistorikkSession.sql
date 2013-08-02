-- AUTHID DEFINER fører til at funksjonene blir kjørt med rettigheter til historikk-db-bruker (brukeren som oppretter denne)
CREATE OR REPLACE PACKAGE SNAPSHOT_TIME AUTHID DEFINER AS

    FUNCTION Get_T_CURRENT RETURN TIMESTAMP;
    -- Finner gjeldende timestamp (default er T_CURRENT )
    FUNCTION Get_T RETURN SNAPSHOT_TRANS.v%TYPE;
    -- Setter gjeldende timestamp
    FUNCTION Set_T(newValue IN SNAPSHOT_TRANS.v%TYPE) RETURN SNAPSHOT_TRANS.v%TYPE;

    -- Definerer timestamp for transaksjon. Denne blir benyttet ved kreering av endringsinnslag ved oppdateringer (insert, update og delete)
    FUNCTION Get_T_Trans RETURN SNAPSHOT_TRANS.v%TYPE;

    -- Konverterer timestamp ifra fast streng-representasjon
    FUNCTION To_T(timestampAsString IN VARCHAR2) RETURN TIMESTAMP;

    -- Bestemmer om gjeldende timestamp er imellom t_Begin (inklusiv) og t_End (eller t_End er T_CURRENT. Returnerer 1 for true, 0 for false
    -- DEPRICATED
    FUNCTION T_Between(t_Begin IN SNAPSHOT_TRANS.v%TYPE, t_End IN SNAPSHOT_TRANS.v%TYPE) RETURN NUMBER;

    Function Get_Bruker Return VARCHAR2;
    Function Set_Bruker (brukernavn IN VARCHAR2) Return VARCHAR2;
    bruker_null EXCEPTION;

END SNAPSHOT_TIME;
/

CREATE OR REPLACE PACKAGE BODY SNAPSHOT_TIME AS

    -- Betegner timestamp for gjeldende snapshot versjon av objektet. Kan betegnes som den versjonen som er 'levende'. Ref tEnd kolonne.
    t_Current CONSTANT SNAPSHOT_TRANS.v%TYPE := SNAPSHOT_TIME.To_T('9999-01-01 00:00:00.00');

    -- initialiserer t slik at man får oppdaterte data i views som standard (se T_Between())
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

    FUNCTION Get_T_Trans RETURN SNAPSHOT_TRANS.v%TYPE IS
    BEGIN
      RETURN HISTORIKK_TRANSACTION.Get_T_Trans();
    END Get_T_Trans;

    FUNCTION To_T(timestampAsString IN VARCHAR2) RETURN TIMESTAMP IS
    BEGIN
      RETURN to_timestamp(timestampAsString, 'YYYY-MM-DD HH24:MI:SS.FF');
    END To_T;

    -- DEPRICATED
    FUNCTION T_Between(t_Begin IN SNAPSHOT_TRANS.v%TYPE, t_End IN SNAPSHOT_TRANS.v%TYPE) RETURN NUMBER IS
    retVal NUMBER;
    BEGIN
      IF (t_Begin<=t AND (t<t_End OR t_End=t_Current))
      THEN
          retVal := 1;
      ELSE
          retVal := 0;
      END IF;
      RETURN retVal;
    END T_Between;

    Function Get_Bruker
    Return VARCHAR2
    is
    begin
      RETURN HISTORIKK_TRANSACTION.Get_Username(true);
    END Get_Bruker;

    Function Set_Bruker(brukernavn IN VARCHAR2)
    Return VARCHAR2
    Is
    BEGIN
      RETURN HISTORIKK_TRANSACTION.Set_Username(brukernavn);
    END Set_Bruker;

END SNAPSHOT_TIME;
/