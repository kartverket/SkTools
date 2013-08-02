-- AUTHID DEFINER fører til at funksjonene blir kjørt med rettigheter til historikk-db-bruker (brukeren som oppretter denne)
CREATE OR REPLACE PACKAGE HISTORIKK_TRANSACTION AUTHID DEFINER AS

  -- Konverterer timestamp ifra fast streng-representasjon
  FUNCTION To_T(timestampAsString IN VARCHAR2) RETURN TIMESTAMP;

  -- Definerer timestamp for transaksjon. Denne blir benyttet ved kreering av endringsinnslag ved oppdateringer (insert, update og delete)
  FUNCTION Get_T_Trans RETURN SNAPSHOT_TRANS.v%TYPE;
  FUNCTION Set_T_Trans(newValue IN SNAPSHOT_TRANS.v%TYPE) RETURN SNAPSHOT_TRANS.v%TYPE;

  Function Get_UserInfo(validate_not_null IN BOOLEAN := FALSE) Return VARCHAR2;
  Function Set_UserInfo(username IN VARCHAR2) Return VARCHAR2;

  bruker_null EXCEPTION;

END HISTORIKK_TRANSACTION;
/

CREATE OR REPLACE PACKAGE BODY HISTORIKK_TRANSACTION AS

  t_Trans SNAPSHOT_TRANS.v%TYPE;
  bruker VARCHAR(255 CHAR);


  FUNCTION To_T(timestampAsString IN VARCHAR2) RETURN TIMESTAMP IS
  BEGIN
    RETURN to_timestamp(timestampAsString, 'YYYY-MM-DD HH24:MI:SS.FF');
  END To_T;


  FUNCTION Get_T_Trans RETURN SNAPSHOT_TRANS.v%TYPE IS
  BEGIN
    BEGIN
      SELECT v INTO t_Trans FROM SNAPSHOT_TRANS WHERE rownum <= 1;
      EXCEPTION
      WHEN NO_DATA_FOUND THEN
         t_Trans := NULL;
     END;
     IF t_Trans IS NULL THEN
       t_Trans := LOCALTIMESTAMP;
       INSERT INTO SNAPSHOT_TRANS VALUES(t_Trans);
     END IF;
    RETURN t_Trans;
  END Get_T_Trans;

  FUNCTION Set_T_Trans(newValue IN SNAPSHOT_TRANS.v%TYPE) RETURN SNAPSHOT_TRANS.v%TYPE IS
  BEGIN
    t_Trans:= newValue;
    RETURN t_Trans;
  END Set_T_Trans;



  FUNCTION Get_UserInfo(validate_not_null IN BOOLEAN := FALSE)
  RETURN VARCHAR2
  IS
  BEGIN
    IF(validate_not_null <> TRUE OR bruker IS NOT NULL )
    THEN
      RETURN bruker;
    ELSE
      RAISE bruker_null;
    END IF;
    EXCEPTION
    WHEN bruker_null
    THEN
      RAISE_APPLICATION_ERROR(-20101, 'Brukernavn må være satt før man prøver å legge inn data!', FALSE);
  END Get_UserInfo;

  FUNCTION Set_UserInfo(username IN VARCHAR2)
  RETURN VARCHAR2
  IS
  BEGIN
     bruker := username;
     RETURN bruker;
  END Set_UserInfo;


END HISTORIKK_TRANSACTION;
/