-- AUTHID DEFINER fører til at funksjonene blir kjørt med rettigheter til historikk-db-bruker (brukeren som oppretter denne)
CREATE OR REPLACE PACKAGE HISTORIKK_TRANSACTION AUTHID DEFINER AS

  -- Konverterer timestamp ifra fast streng-representasjon
  FUNCTION To_T(timestampAsString IN VARCHAR2) RETURN TIMESTAMP;

  -- Definerer timestamp for transaksjon. Denne blir benyttet ved kreering av endringsinnslag ved oppdateringer (insert, update og delete)
  -- localtimestamp_on_missing == TRUE setter LOCALTIMESTAMP som transaksjonsverdi
  FUNCTION Get_T_Trans(localtimestamp_on_missing IN BOOLEAN := TRUE) RETURN SNAPSHOT_TRANS.v%TYPE;
  FUNCTION Set_T_Trans(newValue IN SNAPSHOT_TRANS.v%TYPE, validate_ascending IN BOOLEAN := TRUE, fix_inncorrect_timestamps IN BOOLEAN := FALSE) RETURN SNAPSHOT_TRANS.v%TYPE;

  Function Get_UserInfo(validate_not_null IN BOOLEAN := FALSE) Return VARCHAR2;
  Function Set_UserInfo(username IN VARCHAR2) Return VARCHAR2;

  bruker_null EXCEPTION;
  incorrect_semantics EXCEPTION;

END HISTORIKK_TRANSACTION;
/

CREATE OR REPLACE PACKAGE BODY HISTORIKK_TRANSACTION AS

  t_Trans SNAPSHOT_TRANS.v%TYPE;
  bruker VARCHAR(255 CHAR);


  FUNCTION To_T(timestampAsString IN VARCHAR2) RETURN TIMESTAMP IS
  BEGIN
    RETURN to_timestamp(timestampAsString, 'YYYY-MM-DD HH24:MI:SS.FF');
  END To_T;


  FUNCTION Get_T_Trans(localtimestamp_on_missing IN BOOLEAN := TRUE)
  RETURN SNAPSHOT_TRANS.v%TYPE IS
  BEGIN
    BEGIN
      SELECT v INTO t_Trans FROM SNAPSHOT_TRANS;
      EXCEPTION
      WHEN NO_DATA_FOUND THEN
         t_Trans := NULL;
     END;
     IF t_Trans IS NULL AND localtimestamp_on_missing THEN
       t_Trans := LOCALTIMESTAMP;
       INSERT INTO SNAPSHOT_TRANS VALUES(t_Trans);
     END IF;
    RETURN t_Trans;
  END Get_T_Trans;

  --todo: legge disse til som prosedyrere da en har dml inn i bildet her..
  FUNCTION Set_T_Trans(newValue IN SNAPSHOT_TRANS.v%TYPE, validate_ascending IN BOOLEAN := TRUE, fix_inncorrect_timestamps IN BOOLEAN := FALSE)
  RETURN SNAPSHOT_TRANS.v%TYPE IS
    t_Trans_new SNAPSHOT_TRANS.v%TYPE;
  BEGIN
    IF validate_ascending THEN
      IF newValue IS NULL THEN
        IF t_Trans IS NOT NULL THEN
          RAISE incorrect_semantics;
        END IF;
      ELSE
        IF (t_Trans IS NOT NULL AND newValue < t_Trans) AND NOT fix_inncorrect_timestamps THEN
          RAISE incorrect_semantics;
        END IF;
      END IF;
    END IF;

    t_Trans_new := newValue;

    --må sjekke dette her før en evt overskriver t_Trans ved ny transaksjon...
    IF fix_inncorrect_timestamps AND (t_Trans IS NOT NULL AND newValue <= t_Trans) THEN
      t_Trans_new := t_Trans + NUMTODSINTERVAL(1/1000000000,'SECOND'); --denne må samsvare med granualitet til SNAPSHOT_TRANS.v
      DBMS_OUTPUT.PUT_LINE('adding one ns to last transaction time set!');
    END IF;

    IF Get_T_Trans(false) IS NULL THEN
      INSERT INTO SNAPSHOT_TRANS VALUES (t_Trans_new);
    ELSE
      DBMS_OUTPUT.PUT_LINE('Ignoring multiple updates on t_Trans. Current value is: ' || Get_T_Trans());
    END IF;

    RETURN Get_T_Trans();

    EXCEPTION WHEN incorrect_semantics
    THEN
      RAISE_APPLICATION_ERROR(-20102, 'T for transaksjonen må være stigende!', FALSE);
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

  --todo: legge disse til som prosedyrere da en har dml inn i bildet her..
  FUNCTION Set_UserInfo(username IN VARCHAR2)
  RETURN VARCHAR2
  IS
  BEGIN
     bruker := username;
     RETURN bruker;
  END Set_UserInfo;


END HISTORIKK_TRANSACTION;
/