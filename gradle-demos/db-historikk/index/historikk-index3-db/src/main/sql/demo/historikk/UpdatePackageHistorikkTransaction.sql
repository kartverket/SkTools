-- AUTHID DEFINER fører til at funksjonene blir kjørt med rettigheter til historikk-db-userInfo (brukeren som oppretter/eier denne)
CREATE OR REPLACE PACKAGE "@historikk_index_db_schema@".HISTORIKK_TRANSACTION AUTHID DEFINER AS

  -- Konverterer timestamp ifra fast streng-representasjon
  FUNCTION To_T(timestampAsString IN VARCHAR2) RETURN TIMESTAMP;

  -- Definerer timestamp for transaksjon. Denne blir benyttet ved kreering av endringsinnslag ved oppdateringer (insert, update og delete)
  -- localtimestamp_on_missing == TRUE setter LOCALTIMESTAMP som transaksjonsverdi
  FUNCTION Get_T_Trans(localtimestamp_on_missing IN BOOLEAN := TRUE) RETURN SNAPSHOT_TRANS.v%TYPE;
  PROCEDURE Set_T_Trans(newValue IN SNAPSHOT_TRANS.v%TYPE, opts IN NUMBER := 10);

  incorrect_semantics EXCEPTION;

END HISTORIKK_TRANSACTION;
/

CREATE OR REPLACE PACKAGE BODY "@historikk_index_db_schema@".HISTORIKK_TRANSACTION AS

  -- package private variables and functions

  t_Trans SNAPSHOT_TRANS.v%TYPE;
  



  minimum_unit_of_time CONSTANT INTERVAL DAY(0) TO SECOND(9) := INTERVAL '0.000000001' SECOND; --denne må samsvare med granualitet til SNAPSHOT_TRANS.v


  FUNCTION findLastTransactionFromData
  RETURN SNAPSHOT_TRANS.v%TYPE
  IS
    greatestT SNAPSHOT_TRANS.v%TYPE := NULL;
    max_t_End SNAPSHOT_TRANS.v%TYPE;
    max_t_Begin SNAPSHOT_TRANS.v%TYPE;
  BEGIN

    FOR i IN (SELECT * FROM user_tables WHERE TABLE_NAME LIKE '%\_H' escape '\')
    LOOP
      BEGIN
        EXECUTE IMMEDIATE 'SELECT max(tBegin) FROM ' || i.TABLE_NAME INTO max_t_Begin;   -- todo: substituere tEnd og tBegin med implemententerte navn for løsning
        EXECUTE IMMEDIATE 'SELECT max(tEnd) FROM ' || i.TABLE_NAME || ' WHERE tEnd != :t_Current' INTO max_t_End
          USING SNAPSHOT_TIME.Get_T_CURRENT();

        IF (greatestT IS NOT NULL) THEN
            greatestT := greatest(nvl(max_t_End, greatestT), nvl(max_t_Begin, greatestT), greatestT);
        ELSE
            greatestT := greatest(nvl(max_t_End, max_t_Begin), nvl(max_t_Begin, max_t_End));
        END IF;

        DBMS_OUTPUT.PUT_LINE('Analyzed historikk table ' || i.TABLE_NAME);

      EXCEPTION
        WHEN OTHERS THEN
          DBMS_OUTPUT.PUT_LINE('ERROR: error analyzing historikk table ' || i.TABLE_NAME);
      END;
    END LOOP;

    RETURN greatestT;
  END findLastTransactionFromData;




  -- package impl

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

  -- options & 2 == validate_ascending //GBOK-1858
  -- options & 4 == fix_inncorrect_timestamps  //GBOK-1858
  -- options & 8 : initialize session to latest timestamp //GBOK-2134
  PROCEDURE Set_T_Trans(newValue IN SNAPSHOT_TRANS.v%TYPE, opts IN NUMBER := 10)
  IS
    validate_ascending BOOLEAN := BITAND(opts, 2) <> 0;
    fix_inncorrect_timestamps BOOLEAN := BITAND(opts, 4) <> 0;

    t_Trans_new SNAPSHOT_TRANS.v%TYPE;
    t_Trans_old SNAPSHOT_TRANS.v%TYPE;
    isNewTransaction BOOLEAN;
  BEGIN
    --sanity check of parameters...
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

    t_Trans_old := t_Trans; --tar vare på førtilstand for sessjon..
    isNewTransaction := Get_T_Trans(FALSE) IS NULL;

    IF isNewTransaction THEN
      t_Trans_new := newValue;

      IF t_Trans_old IS NULL THEN
        IF BITAND(opts, 8) <> 0 THEN
          DBMS_OUTPUT.PUT_LINE('searching for previous transaction-time in historikk data...');
          t_Trans_old := findLastTransactionFromData();
          DBMS_OUTPUT.PUT_LINE('...previous transaction-time found: ' || t_Trans_old);
        ELSE
          DBMS_OUTPUT.PUT_LINE('WARNING: previous transaction-time for session is NULL - see GBOK-2134 for details');
        END IF;
      END IF;

      --må sjekke dette her før en evt overskriver t_Trans ved ny transaksjon...
      IF t_Trans_old IS NOT NULL THEN
        --sjekke om det er en ny transaksjon. Korrigerer kun om nødvendig.
        IF fix_inncorrect_timestamps AND newValue <= t_Trans_old THEN
          t_Trans_new := t_Trans_old + minimum_unit_of_time;
          DBMS_OUTPUT.PUT_LINE('adding one ns to last set transaction time!');
        END IF;
      END IF;


      INSERT INTO SNAPSHOT_TRANS VALUES (t_Trans_new);
    ELSE
      DBMS_OUTPUT.PUT_LINE('Ignoring multiple updates on t_Trans. Current value is: ' || Get_T_Trans());
    END IF;

    EXCEPTION WHEN incorrect_semantics
    THEN
      RAISE_APPLICATION_ERROR(-20102, 'T for transaksjonen må være stigende!', FALSE);
  END Set_T_Trans;





END HISTORIKK_TRANSACTION;
/